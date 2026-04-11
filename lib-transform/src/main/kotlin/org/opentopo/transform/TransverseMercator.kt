package org.opentopo.transform

import kotlin.math.*

/**
 * Transverse Mercator projection (forward and inverse) on GRS80.
 */
object TransverseMercator {
    fun forward(
        latDeg: Double,
        lonDeg: Double,
        centralMeridianDeg: Double = 24.0,
        scaleFactor: Double = 0.9996,
        falseEasting: Double = 500_000.0,
        falseNorthing: Double = 0.0,
    ): ProjectedCoordinate {
        val phi = Math.toRadians(latDeg)
        val lam = Math.toRadians(lonDeg)
        val lam0 = Math.toRadians(centralMeridianDeg)

        val a = Ellipsoid.A
        val e2 = Ellipsoid.E2
        val ePrime2 = e2 / (1.0 - e2)

        val sinPhi = sin(phi)
        val cosPhi = cos(phi)
        val tanPhi = tan(phi)

        val n = a / sqrt(1.0 - e2 * sinPhi * sinPhi)
        val t = tanPhi * tanPhi
        val c = ePrime2 * cosPhi * cosPhi
        val aa = (lam - lam0) * cosPhi

        // Meridional arc length
        val e4 = e2 * e2
        val e6 = e2 * e2 * e2
        val m = a * (
            (1.0 - e2 / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0) * phi
            - (3.0 * e2 / 8.0 + 3.0 * e4 / 32.0 + 45.0 * e6 / 1024.0) * sin(2.0 * phi)
            + (15.0 * e4 / 256.0 + 45.0 * e6 / 1024.0) * sin(4.0 * phi)
            - (35.0 * e6 / 3072.0) * sin(6.0 * phi)
        )

        val easting = falseEasting + scaleFactor * n * (
            aa
            + (1.0 - t + c) * aa.pow(3) / 6.0
            + (5.0 - 18.0 * t + t * t + 72.0 * c - 58.0 * ePrime2) * aa.pow(5) / 120.0
        )

        val northing = falseNorthing + scaleFactor * (
            m + n * tanPhi * (
                aa * aa / 2.0
                + (5.0 - t + 9.0 * c + 4.0 * c * c) * aa.pow(4) / 24.0
                + (61.0 - 58.0 * t + t * t + 600.0 * c - 330.0 * ePrime2) * aa.pow(6) / 720.0
            )
        )

        return ProjectedCoordinate(eastingM = easting, northingM = northing)
    }
}
