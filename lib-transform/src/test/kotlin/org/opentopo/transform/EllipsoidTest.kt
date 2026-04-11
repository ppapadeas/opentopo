package org.opentopo.transform

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class EllipsoidTest {

    @Test
    fun `geographic to cartesian - Athens`() {
        val geo = GeographicCoordinate(37.9715, 23.7267, 150.0)
        val xyz = Ellipsoid.geographicToCartesian(geo)
        assertTrue(abs(xyz.x - 4608954.809455) < 0.001, "X mismatch: ${xyz.x}")
        assertTrue(abs(xyz.y - 2025751.888171) < 0.001, "Y mismatch: ${xyz.y}")
        assertTrue(abs(xyz.z - 3903042.987616) < 0.001, "Z mismatch: ${xyz.z}")
    }

    @Test
    fun `cartesian to geographic - Athens`() {
        val xyz = CartesianCoordinate(4609156.543782, 2025677.988848, 3902798.575022)
        val geo = Ellipsoid.cartesianToGeographic(xyz)
        assertTrue(abs(geo.latitudeDeg - 37.968905265531) < 1e-9, "lat mismatch: ${geo.latitudeDeg}")
        assertTrue(abs(geo.longitudeDeg - 23.725006319766) < 1e-9, "lon mismatch: ${geo.longitudeDeg}")
        assertTrue(abs(geo.heightM - 121.776448) < 0.001, "h mismatch: ${geo.heightM}")
    }

    @Test
    fun `round-trip geographic - cartesian - geographic`() {
        val original = GeographicCoordinate(40.6401, 22.9444, 30.0)
        val xyz = Ellipsoid.geographicToCartesian(original)
        val roundTrip = Ellipsoid.cartesianToGeographic(xyz)
        assertTrue(abs(roundTrip.latitudeDeg - original.latitudeDeg) < 1e-10)
        assertTrue(abs(roundTrip.longitudeDeg - original.longitudeDeg) < 1e-10)
        assertTrue(abs(roundTrip.heightM - original.heightM) < 1e-6)
    }
}
