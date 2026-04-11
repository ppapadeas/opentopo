package org.opentopo.transform

import kotlin.math.*

/**
 * GRS80 ellipsoid constants and geographic/Cartesian conversion.
 */
object Ellipsoid {
    const val A: Double = 6378137.0              // semi-major axis (m)
    const val F: Double = 1.0 / 298.257222101    // flattening
    val E2: Double = 2 * F - F * F               // first eccentricity squared

    fun geographicToCartesian(coord: GeographicCoordinate): CartesianCoordinate {
        val phi = Math.toRadians(coord.latitudeDeg)
        val lam = Math.toRadians(coord.longitudeDeg)
        val sinPhi = sin(phi)
        val cosPhi = cos(phi)
        val n = A / sqrt(1.0 - E2 * sinPhi * sinPhi)
        return CartesianCoordinate(
            x = (n + coord.heightM) * cosPhi * cos(lam),
            y = (n + coord.heightM) * cosPhi * sin(lam),
            z = (n * (1.0 - E2) + coord.heightM) * sinPhi,
        )
    }

    fun cartesianToGeographic(coord: CartesianCoordinate): GeographicCoordinate {
        val p = sqrt(coord.x * coord.x + coord.y * coord.y)
        val lam = atan2(coord.y, coord.x)
        // Iterative method (Bowring)
        var phi = atan2(coord.z, p * (1.0 - E2))
        for (i in 0 until 10) {
            val n = A / sqrt(1.0 - E2 * sin(phi) * sin(phi))
            phi = atan2(coord.z + E2 * n * sin(phi), p)
        }
        val n = A / sqrt(1.0 - E2 * sin(phi) * sin(phi))
        val h = if (abs(cos(phi)) > 1e-10) {
            p / cos(phi) - n
        } else {
            abs(coord.z) / sin(phi) - n * (1.0 - E2)
        }
        return GeographicCoordinate(
            latitudeDeg = Math.toDegrees(phi),
            longitudeDeg = Math.toDegrees(lam),
            heightM = h,
        )
    }
}
