package org.opentopo.transform

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class CorrectionGridTest {

    private fun loadGrid(name: String): CorrectionGrid {
        val stream = javaClass.classLoader.getResourceAsStream(name)
            ?: throw IllegalStateException("Grid file $name not found in test resources")
        return CorrectionGrid(stream)
    }

    @Test
    fun `parse dE grid header`() {
        val grid = loadGrid("dE_2km_V1-0.grd")
        assertEquals(408, grid.nRows)
        assertEquals(422, grid.nCols)
        assertEquals(2000.0, grid.cellSize)
        assertEquals(1845619.0, grid.swNorthing)
        assertEquals(41600.0, grid.swEasting)
    }

    @Test
    fun `parse dN grid header`() {
        val grid = loadGrid("dN_2km_V1-0.grd")
        assertEquals(408, grid.nRows)
        assertEquals(422, grid.nCols)
    }

    @Test
    fun `interpolate dE at Athens TM07`() {
        val grid = loadGrid("dE_2km_V1-0.grd")
        val deCm = grid.interpolate(475995.684650, 2202688.117793)
        assertTrue(abs(deCm - 34.071843) < 0.01, "dE mismatch: $deCm")
    }

    @Test
    fun `interpolate dN at Athens TM07`() {
        val grid = loadGrid("dN_2km_V1-0.grd")
        val dnCm = grid.interpolate(475995.684650, 2202688.117793)
        assertTrue(abs(dnCm - 48.029711) < 0.01, "dN mismatch: $dnCm")
    }

    @Test
    fun `out of bounds throws`() {
        val grid = loadGrid("dE_2km_V1-0.grd")
        assertFailsWith<IllegalArgumentException> {
            grid.interpolate(0.0, 0.0)
        }
    }
}
