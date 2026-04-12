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
 */
class HeposTransform(
    gridDeStream: InputStream,
    gridDnStream: InputStream,
) {
    private val gridDe = CorrectionGrid(gridDeStream)
    private val gridDn = CorrectionGrid(gridDnStream)

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
        val orthoHeight = if (geoidSeparation != null) coord.heightM - geoidSeparation else null
        return TransformResult(coord, xyz, xyzEgsa, geoEgsa, tm87, tm07, deCm, dnCm, output, geoidSeparation, orthoHeight)
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
