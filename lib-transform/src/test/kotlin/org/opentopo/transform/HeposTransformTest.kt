package org.opentopo.transform

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class HeposTransformTest {

    private fun createTransform(): HeposTransform {
        val deStream = javaClass.classLoader.getResourceAsStream("dE_2km_V1-0.grd")
            ?: throw IllegalStateException("dE grid not found in test resources")
        val dnStream = javaClass.classLoader.getResourceAsStream("dN_2km_V1-0.grd")
            ?: throw IllegalStateException("dN grid not found in test resources")
        return HeposTransform(deStream, dnStream)
    }

    private fun assertTransform(
        name: String,
        lat: Double,
        lon: Double,
        h: Double,
        expectedE: Double,
        expectedN: Double,
        toleranceM: Double = 0.01,
    ) {
        val transform = createTransform()
        val result = transform.forward(GeographicCoordinate(lat, lon, h))
        val dE = abs(result.eastingM - expectedE)
        val dN = abs(result.northingM - expectedN)
        assertTrue(dE < toleranceM, "$name: E off by ${dE}m (got ${result.eastingM}, expected $expectedE)")
        assertTrue(dN < toleranceM, "$name: N off by ${dN}m (got ${result.northingM}, expected $expectedN)")
    }

    @Test fun `Athens Acropolis`() = assertTransform(
        "Athens", 37.9715, 23.7267, 150.0, 475846.416695, 4202401.145493,
    )

    @Test fun `Thessaloniki`() = assertTransform(
        "Thessaloniki", 40.6401, 22.9444, 30.0, 410590.254044, 4499055.447598,
    )

    @Test fun `Kalamata`() = assertTransform(
        "Kalamata", 37.0388, 22.1143, 10.0, 332145.258386, 4100551.548426,
    )

    @Test fun `Heraklion Crete`() = assertTransform(
        "Heraklion", 35.3387, 25.1442, 20.0, 603830.795259, 3910917.557732,
    )

    @Test fun `Corfu`() = assertTransform(
        "Corfu", 39.6243, 19.9217, 5.0, 149769.169009, 4393724.733123,
    )

    @Test fun `Rhodes`() = assertTransform(
        "Rhodes", 36.4341, 28.2176, 15.0, 877980.791062, 4040083.307377,
    )

    @Test fun `Ioannina`() = assertTransform(
        "Ioannina", 39.6650, 20.8537, 480.0, 229947.472091, 4395021.924261,
    )

    @Test fun `Patras`() = assertTransform(
        "Patras", 38.2466, 21.7346, 3.0, 301609.144605, 4235315.878419,
    )

    @Test fun `Alexandroupoli`() = assertTransform(
        "Alexandroupoli", 40.8469, 25.8743, 5.0, 657851.369311, 4523166.416795,
    )

    @Test fun `Lamia`() = assertTransform(
        "Lamia", 38.8991, 22.4342, 50.0, 364068.124759, 4306457.554471,
    )
}
