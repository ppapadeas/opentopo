package org.opentopo.transform

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class HelmertTest {

    @Test
    fun `helmert forward - Athens HTRS07 to EGSA87`() {
        val source = CartesianCoordinate(4608954.809455, 2025751.888171, 3903042.987616)
        val result = Helmert.forward(source)
        assertTrue(abs(result.x - 4609156.543782) < 0.001, "X mismatch: ${result.x}")
        assertTrue(abs(result.y - 2025677.988848) < 0.001, "Y mismatch: ${result.y}")
        assertTrue(abs(result.z - 3902798.575022) < 0.001, "Z mismatch: ${result.z}")
    }

    @Test
    fun `helmert roundtrip - forward then inverse recovers original`() {
        val source = CartesianCoordinate(4608954.809455, 2025751.888171, 3903042.987616)
        val egsa = Helmert.forward(source)
        val recovered = Helmert.inverse(egsa)
        assertTrue(abs(recovered.x - source.x) < 0.001, "X roundtrip off: ${abs(recovered.x - source.x)}")
        assertTrue(abs(recovered.y - source.y) < 0.001, "Y roundtrip off: ${abs(recovered.y - source.y)}")
        assertTrue(abs(recovered.z - source.z) < 0.001, "Z roundtrip off: ${abs(recovered.z - source.z)}")
    }
}
