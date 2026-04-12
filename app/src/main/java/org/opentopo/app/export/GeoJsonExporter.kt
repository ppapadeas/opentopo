package org.opentopo.app.export

import org.opentopo.app.db.PointEntity
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports survey points as GeoJSON FeatureCollection.
 * Coordinates are in EGSA87/GGRS87 (EPSG:2100).
 */
object GeoJsonExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    fun export(points: List<PointEntity>, projectName: String, output: OutputStream) {
        val writer = OutputStreamWriter(output, Charsets.UTF_8)

        writer.write("""{"type":"FeatureCollection","name":""")
        writer.write(jsonString(projectName))
        writer.write(""","crs":{"type":"name","properties":{"name":"urn:ogc:def:crs:EPSG::2100"}}""")
        writer.write(""","features":[""")

        points.forEachIndexed { index, p ->
            if (index > 0) writer.write(",")
            writer.write("""{"type":"Feature","properties":{""")
            writer.write(""""id":${jsonString(p.pointId)}""")
            writer.write(""","lat_wgs84":${p.latitude}""")
            writer.write(""","lon_wgs84":${p.longitude}""")
            p.altitude?.let { writer.write(""","altitude":${"%.3f".format(it)}""") }
            p.orthometricHeight?.let { writer.write(""","ortho_height":${"%.3f".format(it)}""") }
            p.geoidSeparation?.let { writer.write(""","geoid_n":${"%.3f".format(it)}""") }
            p.horizontalAccuracy?.let { writer.write(""","h_accuracy":${"%.3f".format(it)}""") }
            p.verticalAccuracy?.let { writer.write(""","v_accuracy":${"%.3f".format(it)}""") }
            writer.write(""","fix":"${fixLabel(p.fixQuality)}"""")
            writer.write(""","satellites":${p.numSatellites}""")
            writer.write(""","timestamp":${jsonString(dateFormat.format(Date(p.timestamp)))}""")
            if (p.remarks.isNotBlank()) {
                writer.write(""","remarks":${jsonString(p.remarks)}""")
            }
            writer.write("}")  // end properties

            val e = p.easting
            val n = p.northing
            if (e != null && n != null) {
                writer.write(""","geometry":{"type":"Point","coordinates":[${"%.3f".format(e)},${"%.3f".format(n)}]}""")
            } else {
                writer.write(""","geometry":null""")
            }
            writer.write("}")  // end feature
        }

        writer.write("]}")
        writer.flush()
    }

    private fun fixLabel(quality: Int): String = when (quality) {
        4 -> "RTK_Fix"
        5 -> "RTK_Float"
        2 -> "DGPS"
        1 -> "GPS"
        else -> "None"
    }

    private fun jsonString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
