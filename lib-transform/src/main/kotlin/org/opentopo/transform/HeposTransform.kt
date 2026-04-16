package org.opentopo.transform

import java.io.InputStream

/**
 * Full HEPOS transformation pipeline: WGS84/HTRS07 -> GGRS87/EGSA87 (EPSG:2100).
 *
 * Steps:
 *  1. Geographic -> Cartesian (HTRS07, GRS80)
 *  2. 7-parameter Helmert (HTRS07 -> EGSA87 Cartesian)
 *  3. Cartesian -> Geographic (EGSA87, GRS80)
 *  4. Geographic -> TM87 projection (approximate EGSA87)
 *  5. Compute TM07 coords from original HTRS07 for grid lookup
 *  6. Bilinear grid interpolation + apply corrections
 *  7. (optional) Greek geoid interpolation for orthometric height
 */
class HeposTransform(
    gridDeStream: InputStream,
    gridDnStream: InputStream,
    geoidGridStream: InputStream? = null,
) {
    private val gridDe = CorrectionGrid(gridDeStream)
    private val gridDn = CorrectionGrid(gridDnStream)
    private val gridGeoid: CorrectionGrid? = geoidGridStream?.let { CorrectionGrid(it) }

    /** Whether a Greek geoid grid is loaded. */
    val hasGeoidGrid: Boolean get() = gridGeoid != null

    /**
     * Interpolate Greek geoid undulation at the given TM07 coordinates.
     * @return geoid undulation N in metres, or null if no geoid grid loaded
     */
    fun geoidUndulation(tm07Easting: Double, tm07Northing: Double): Double? {
        return gridGeoid?.interpolate(tm07Easting, tm07Northing)
    }

    /** Forward transform with all intermediate results exposed. */
    fun forwardDetailed(coord: GeographicCoordinate, geoidSeparation: Double? = null): TransformResult {
        val xyz = Ellipsoid.geographicToCartesian(coord)
        val xyzEgsa = Helmert.forward(xyz)
        val geoEgsa = Ellipsoid.cartesianToGeographic(xyzEgsa)
        val tm87 = TransverseMercator.forward(geoEgsa.latitudeDeg, geoEgsa.longitudeDeg, 24.0, 0.9996, 500_000.0, 0.0)
        val tm07 = TransverseMercator.forward(coord.latitudeDeg, coord.longitudeDeg, 24.0, 0.9996, 500_000.0, -2_000_000.0)
        val deCm = gridDe.interpolate(tm07.eastingM, tm07.northingM)
        val dnCm = gridDn.interpolate(tm07.eastingM, tm07.northingM)
        val output = ProjectedCoordinate(tm87.eastingM + deCm / 100.0, tm87.northingM + dnCm / 100.0)
        // Prefer Greek geoid for orthometric height; fall back to receiver geoid separation
        val greekGeoidN = gridGeoid?.interpolate(tm07.eastingM, tm07.northingM)
        val effectiveN = greekGeoidN ?: geoidSeparation
        val orthoHeight = if (effectiveN != null) coord.heightM - effectiveN else null
        return TransformResult(coord, xyz, xyzEgsa, geoEgsa, tm87, tm07, deCm, dnCm, output, effectiveN, orthoHeight)
    }

    /**
     * Reverse transform: EGSA87 (E, N) -> WGS84/HTRS07 geographic.
     *
     * @param approximateHeightM Approximate WGS84 ellipsoidal height in metres.
     *   TM projection discards height, so providing one improves accuracy from
     *   ~20 mm (at h=500 m, worst case) to sub-millimetre. For most map-rendering
     *   uses, the default of 0.0 is sufficient.
     */
    fun reverse(coord: ProjectedCoordinate, approximateHeightM: Double = 0.0): GeographicCoordinate {
        // Estimate EGSA87 ellipsoidal height from the WGS84 height by applying
        // the Helmert height shift (≈ −24 m in Greece).
        val egsa87Height = if (approximateHeightM != 0.0) {
            val approxGeo = GeographicCoordinate(38.0, 24.0, approximateHeightM)
            val h = Ellipsoid.cartesianToGeographic(
                Helmert.forward(Ellipsoid.geographicToCartesian(approxGeo)),
            ).heightM
            h
        } else {
            0.0
        }

        var tm87E = coord.eastingM
        var tm87N = coord.northingM

        for (i in 0 until 10) {
            val tmGeo = TransverseMercator.inverse(tm87E, tm87N)
            val geoEgsa = GeographicCoordinate(tmGeo.latitudeDeg, tmGeo.longitudeDeg, egsa87Height)
            val xyzHtrs = Helmert.inverse(Ellipsoid.geographicToCartesian(geoEgsa))
            val wgs84 = Ellipsoid.cartesianToGeographic(xyzHtrs)

            val tm07 = TransverseMercator.forward(
                latDeg = wgs84.latitudeDeg,
                lonDeg = wgs84.longitudeDeg,
                centralMeridianDeg = 24.0,
                scaleFactor = 0.9996,
                falseEasting = 500_000.0,
                falseNorthing = -2_000_000.0,
            )
            val deCm = gridDe.interpolate(tm07.eastingM, tm07.northingM)
            val dnCm = gridDn.interpolate(tm07.eastingM, tm07.northingM)

            val newTm87E = coord.eastingM - deCm / 100.0
            val newTm87N = coord.northingM - dnCm / 100.0

            if (kotlin.math.abs(newTm87E - tm87E) < 1e-9 && kotlin.math.abs(newTm87N - tm87N) < 1e-9) {
                return wgs84
            }
            tm87E = newTm87E
            tm87N = newTm87N
        }

        val tmGeo = TransverseMercator.inverse(tm87E, tm87N)
        val geoEgsa = GeographicCoordinate(tmGeo.latitudeDeg, tmGeo.longitudeDeg, egsa87Height)
        val xyzHtrs = Helmert.inverse(Ellipsoid.geographicToCartesian(geoEgsa))
        return Ellipsoid.cartesianToGeographic(xyzHtrs)
    }

    fun forward(coord: GeographicCoordinate): ProjectedCoordinate {
        // Step 1: Geographic -> Cartesian (HTRS07)
        val xyz = Ellipsoid.geographicToCartesian(coord)

        // Step 2: 7-parameter Helmert (HTRS07 -> EGSA87 Cartesian)
        val xyzEgsa = Helmert.forward(xyz)

        // Step 3: Cartesian -> Geographic (EGSA87)
        val geoEgsa = Ellipsoid.cartesianToGeographic(xyzEgsa)

        // Step 4: Geographic -> TM87 projection (approximate EGSA87)
        val tm87 = TransverseMercator.forward(
            latDeg = geoEgsa.latitudeDeg,
            lonDeg = geoEgsa.longitudeDeg,
            centralMeridianDeg = 24.0,
            scaleFactor = 0.9996,
            falseEasting = 500_000.0,
            falseNorthing = 0.0,
        )

        // Step 5: Project ORIGINAL HTRS07 coords to TM07 for grid lookup
        val tm07 = TransverseMercator.forward(
            latDeg = coord.latitudeDeg,
            lonDeg = coord.longitudeDeg,
            centralMeridianDeg = 24.0,
            scaleFactor = 0.9996,
            falseEasting = 500_000.0,
            falseNorthing = -2_000_000.0,
        )

        // Step 6: Grid interpolation + apply corrections
        val deCm = gridDe.interpolate(tm07.eastingM, tm07.northingM)
        val dnCm = gridDn.interpolate(tm07.eastingM, tm07.northingM)

        return ProjectedCoordinate(
            eastingM = tm87.eastingM + deCm / 100.0,
            northingM = tm87.northingM + dnCm / 100.0,
        )
    }
}
