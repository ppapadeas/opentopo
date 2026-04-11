package org.opentopo.transform

import java.io.InputStream

/**
 * HEPOS correction grid parser and bilinear interpolator.
 *
 * Grid format (.grd):
 *   Line 0: nrows (408, Northing direction S->N)
 *   Line 1: ncols (422, Easting direction W->E)
 *   Line 2: cellsize (2000.0 m)
 *   Line 3: TM07 Northing of SW corner (1845619.0)
 *   Line 4: TM07 Easting of SW corner (41600.0)
 *   Line 5+: data values in cm, row-major from SW corner
 */
class CorrectionGrid(inputStream: InputStream) {
    val nRows: Int
    val nCols: Int
    val cellSize: Double
    val swNorthing: Double
    val swEasting: Double
    private val data: DoubleArray

    init {
        val reader = inputStream.bufferedReader()
        val lines = reader.readLines()
        reader.close()

        nRows = lines[0].trim().toInt()
        nCols = lines[1].trim().toInt()
        cellSize = lines[2].trim().toDouble()
        swNorthing = lines[3].trim().toDouble()
        swEasting = lines[4].trim().toDouble()

        // Parse all data values into a flat array (row-major, row 0 = south)
        val values = mutableListOf<Double>()
        for (i in 5 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            for (token in line.split("\\s+".toRegex())) {
                values.add(token.toDouble())
            }
        }

        val expected = nRows * nCols
        require(values.size >= expected) {
            "Grid has ${values.size} values, expected $expected"
        }

        data = values.toDoubleArray()
    }

    /**
     * Bilinear interpolation at the given TM07 coordinates.
     * @return correction value in centimetres
     * @throws IllegalArgumentException if the point is outside grid bounds
     */
    fun interpolate(tm07Easting: Double, tm07Northing: Double): Double {
        val col = (tm07Easting - swEasting) / cellSize
        val row = (tm07Northing - swNorthing) / cellSize

        require(col >= 0 && col < nCols - 1 && row >= 0 && row < nRows - 1) {
            "Point TM07 E=$tm07Easting N=$tm07Northing outside grid bounds " +
                "(E: $swEasting\u2013${swEasting + (nCols - 1) * cellSize}, " +
                "N: $swNorthing\u2013${swNorthing + (nRows - 1) * cellSize})"
        }

        val c0 = col.toInt()
        val r0 = row.toInt()
        val dc = col - c0
        val dr = row - r0

        val v00 = data[r0 * nCols + c0]
        val v01 = data[r0 * nCols + c0 + 1]
        val v10 = data[(r0 + 1) * nCols + c0]
        val v11 = data[(r0 + 1) * nCols + c0 + 1]

        return v00 * (1 - dc) * (1 - dr) +
            v01 * dc * (1 - dr) +
            v10 * (1 - dc) * dr +
            v11 * dc * dr
    }
}
