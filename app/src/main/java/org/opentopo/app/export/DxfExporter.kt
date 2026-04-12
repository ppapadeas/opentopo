package org.opentopo.app.export

import org.opentopo.app.db.PointEntity
import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * Exports survey points to DXF R12 (AC1009).
 *
 * DXF R12 is the most universally compatible format — no handles,
 * no CLASSES/BLOCKS/OBJECTS sections required. Group codes are
 * right-justified to 3 characters per DXF spec.
 */
object DxfExporter {

    fun export(points: List<PointEntity>, output: OutputStream) {
        val sb = StringBuilder()

        // ── HEADER ──
        sb.g(0, "SECTION"); sb.g(2, "HEADER")
        sb.g(9, "\$ACADVER"); sb.g(1, "AC1009")
        sb.g(0, "ENDSEC")

        // ── TABLES ──
        sb.g(0, "SECTION"); sb.g(2, "TABLES")

        // VPORT table (required by AutoCAD)
        sb.g(0, "TABLE"); sb.g(2, "VPORT"); sb.g(70, "0"); sb.g(0, "ENDTAB")

        // LTYPE table
        sb.g(0, "TABLE"); sb.g(2, "LTYPE"); sb.g(70, "1")
        sb.g(0, "LTYPE"); sb.g(2, "CONTINUOUS"); sb.g(70, "0")
        sb.g(3, "Solid line"); sb.g(72, "65"); sb.g(73, "0"); sb.g(40, "0.0")
        sb.g(0, "ENDTAB")

        // LAYER table
        sb.g(0, "TABLE"); sb.g(2, "LAYER"); sb.g(70, "1")
        sb.g(0, "LAYER"); sb.g(2, "SURVEY"); sb.g(70, "0"); sb.g(62, "3"); sb.g(6, "CONTINUOUS")
        sb.g(0, "ENDTAB")

        // STYLE table
        sb.g(0, "TABLE"); sb.g(2, "STYLE"); sb.g(70, "1")
        sb.g(0, "STYLE"); sb.g(2, "STANDARD"); sb.g(70, "0")
        sb.g(40, "0.0"); sb.g(41, "1.0"); sb.g(50, "0.0"); sb.g(71, "0"); sb.g(42, "2.5")
        sb.g(3, "txt"); sb.g(4, "")
        sb.g(0, "ENDTAB")

        // VIEW table (required by AutoCAD)
        sb.g(0, "TABLE"); sb.g(2, "VIEW"); sb.g(70, "0"); sb.g(0, "ENDTAB")

        // UCS table (required by AutoCAD)
        sb.g(0, "TABLE"); sb.g(2, "UCS"); sb.g(70, "0"); sb.g(0, "ENDTAB")

        // APPID table (required by AutoCAD)
        sb.g(0, "TABLE"); sb.g(2, "APPID"); sb.g(70, "1")
        sb.g(0, "APPID"); sb.g(2, "ACAD"); sb.g(70, "0")
        sb.g(0, "ENDTAB")

        // DIMSTYLE table (required by AutoCAD)
        sb.g(0, "TABLE"); sb.g(2, "DIMSTYLE"); sb.g(70, "0"); sb.g(0, "ENDTAB")

        sb.g(0, "ENDSEC")

        // ── BLOCKS (empty, some readers require it) ──
        sb.g(0, "SECTION"); sb.g(2, "BLOCKS"); sb.g(0, "ENDSEC")

        // ── ENTITIES ──
        sb.g(0, "SECTION"); sb.g(2, "ENTITIES")

        for (p in points) {
            if (p.layerType != "point") continue
            val e = p.easting ?: continue
            val n = p.northing ?: continue
            val z = p.altitude ?: 0.0

            // POINT entity
            sb.g(0, "POINT"); sb.g(8, "SURVEY")
            sb.g(10, "%.3f".format(e))
            sb.g(20, "%.3f".format(n))
            sb.g(30, "%.3f".format(z))

            // TEXT label
            sb.g(0, "TEXT"); sb.g(8, "SURVEY"); sb.g(7, "STANDARD")
            sb.g(10, "%.3f".format(e + 1.0))
            sb.g(20, "%.3f".format(n + 1.0))
            sb.g(30, "%.3f".format(z))
            sb.g(40, "1.5")
            sb.g(1, p.pointId)
        }

        sb.g(0, "ENDSEC")
        sb.g(0, "EOF")

        // Write as ASCII (DXF R12 spec)
        output.write(sb.toString().toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    /** Append a DXF group code (right-justified to 3 chars) + value. */
    private fun StringBuilder.g(code: Int, value: String) {
        append("%3d\n%s\n".format(code, value))
    }
}
