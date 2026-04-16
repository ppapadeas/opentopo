package org.opentopo.transform

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class TransverseMercatorTest {

    @Test
    fun `TM87 forward - Athens EGSA87 geographic`() {
        val result = TransverseMercator.forward(
            latDeg = 37.968905265531,
            lonDeg = 23.725006319766,
            centralMeridianDeg = 24.0,
            scaleFactor = 0.9996,
            falseEasting = 500_000.0,
            falseNorthing = 0.0,
        )
        assertTrue(abs(result.eastingM - 475846.075977) < 0.001, "E mismatch: ${result.eastingM}")
        assertTrue(abs(result.northingM - 4202400.665196) < 0.001, "N mismatch: ${result.northingM}")
    }

    @Test
    fun `TM07 forward - Athens HTRS07 geographic`() {
        val result = TransverseMercator.forward(
            latDeg = 37.9715,
            lonDeg = 23.7267,
            centralMeridianDeg = 24.0,
            scaleFactor = 0.9996,
            falseEasting = 500_000.0,
            falseNorthing = -2_000_000.0,
        )
        assertTrue(abs(result.eastingM - 475995.684650) < 0.001, "E mismatch: ${result.eastingM}")
        assertTrue(abs(result.northingM - 2202688.117793) < 0.001, "N mismatch: ${result.northingM}")
    }

    @Test
    fun `TM87 inverse - recovers Athens EGSA87 geographic`() {
        val geo = TransverseMercator.inverse(
            eastingM = 475846.075977,
            northingM = 4202400.665196,
            centralMeridianDeg = 24.0,
            scaleFactor = 0.9996,
            falseEasting = 500_000.0,
            falseNorthing = 0.0,
        )
        assertTrue(abs(geo.latitudeDeg - 37.968905265531) < 1e-8, "Lat mismatch: ${geo.latitudeDeg}")
        assertTrue(abs(geo.longitudeDeg - 23.725006319766) < 1e-8, "Lon mismatch: ${geo.longitudeDeg}")
    }

    @Test
    fun `TM87 roundtrip - forward then inverse`() {
        val lat = 37.968905265531
        val lon = 23.725006319766
        val projected = TransverseMercator.forward(lat, lon)
        val recovered = TransverseMercator.inverse(projected.eastingM, projected.northingM)
        assertTrue(abs(recovered.latitudeDeg - lat) < 1e-9, "Lat roundtrip off: ${abs(recovered.latitudeDeg - lat)}")
        assertTrue(abs(recovered.longitudeDeg - lon) < 1e-9, "Lon roundtrip off: ${abs(recovered.longitudeDeg - lon)}")
    }
}
