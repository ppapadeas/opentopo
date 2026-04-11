package org.opentopo.app.export

import org.opentopo.app.db.PointEntity
import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * Exports survey points to DXF format.
 * Points are exported as POINT entities with EGSA87 coordinates.
 * Point IDs are placed as TEXT entities next to each point.
 */
object DxfExporter {

    fun export(points: List<PointEntity>, output: OutputStream) {
        val writer = OutputStreamWriter(output, Charsets.UTF_8)

        // Header
        writer.write("0\nSECTION\n2\nHEADER\n")
        writer.write("9\n\$ACADVER\n1\nAC1014\n")  // AutoCAD R14
        writer.write("0\nENDSEC\n")

        // Tables (minimal — define a layer)
        writer.write("0\nSECTION\n2\nTABLES\n")
        writer.write("0\nTABLE\n2\nLAYER\n70\n1\n")
        writer.write("0\nLAYER\n2\nPOINTS\n70\n0\n62\n7\n6\nCONTINUOUS\n")
        writer.write("0\nENDTAB\n")
        writer.write("0\nENDSEC\n")

        // Entities
        writer.write("0\nSECTION\n2\nENTITIES\n")

        for (p in points) {
            val e = p.easting ?: continue
            val n = p.northing ?: continue
            val z = p.altitude ?: 0.0

            // POINT entity
            writer.write("0\nPOINT\n")
            writer.write("8\nPOINTS\n")  // layer
            writer.write("10\n${"%.3f".format(e)}\n")  // X
            writer.write("20\n${"%.3f".format(n)}\n")  // Y
            writer.write("30\n${"%.3f".format(z)}\n")  // Z

            // TEXT entity for label
            writer.write("0\nTEXT\n")
            writer.write("8\nPOINTS\n")
            writer.write("10\n${"%.3f".format(e + 0.5)}\n")
            writer.write("20\n${"%.3f".format(n + 0.5)}\n")
            writer.write("30\n0.0\n")
            writer.write("40\n1.0\n")  // text height
            writer.write("1\n${p.pointId}\n")
        }

        writer.write("0\nENDSEC\n")
        writer.write("0\nEOF\n")
        writer.flush()
    }
}
