package org.opentopo.transform

import kotlin.math.PI

/**
 * 7-parameter Helmert similarity transformation.
 * Uses the official HEPOS parameters (HTRS07 -> EGSA87).
 *
 * Source: HEPOS_coord_transf_model_summary_081107_gr.pdf, equation (1).
 */
object Helmert {
    // Translation parameters (metres)
    private const val TX = 203.437
    private const val TY = -73.461
    private const val TZ = -243.594

    // Rotation parameters (arcseconds -> radians)
    private val EX = -0.170 * PI / (180.0 * 3600.0)
    private val EY = -0.060 * PI / (180.0 * 3600.0)
    private val EZ = -0.151 * PI / (180.0 * 3600.0)

    // Scale difference (ppm -> dimensionless)
    private const val DS = -0.294e-6

    fun forward(source: CartesianCoordinate): CartesianCoordinate {
        val (x, y, z) = source
        return CartesianCoordinate(
            x = x + TX + DS * x + EZ * y - EY * z,
            y = y + TY - EZ * x + DS * y + EX * z,
            z = z + TZ + EY * x - EX * y + DS * z,
        )
    }
}
