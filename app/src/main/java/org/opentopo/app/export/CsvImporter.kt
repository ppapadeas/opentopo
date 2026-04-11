package org.opentopo.app.export

import org.opentopo.app.db.PointEntity
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Imports survey points from CSV files produced by [CsvExporter].
 *
 * Expected column order (matching CsvExporter):
 * ID, Easting_EGSA87, Northing_EGSA87, Latitude_WGS84, Longitude_WGS84,
 * Altitude, H_Accuracy, V_Accuracy, Fix, Satellites, HDOP, Averaging_s,
 * DateTime, Remarks
 */
object CsvImporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun import(input: InputStream, projectId: Long): List<PointEntity> {
        val reader = input.bufferedReader()
        val header = reader.readLine() ?: return emptyList() // skip header

        return reader.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line -> parseLine(line, projectId) }
            .toList()
    }

    private fun parseLine(line: String, projectId: Long): PointEntity? {
        val fields = parseCsvFields(line)
        if (fields.size < 5) return null
        return try {
            PointEntity(
                projectId = projectId,
                pointId = fields[0].trim(),
                easting = fields.getOrNull(1)?.trim()?.toDoubleOrNull(),
                northing = fields.getOrNull(2)?.trim()?.toDoubleOrNull(),
                latitude = fields[3].trim().toDoubleOrNull() ?: return null,
                longitude = fields[4].trim().toDoubleOrNull() ?: return null,
                altitude = fields.getOrNull(5)?.trim()?.toDoubleOrNull(),
                horizontalAccuracy = fields.getOrNull(6)?.trim()?.toDoubleOrNull(),
                verticalAccuracy = fields.getOrNull(7)?.trim()?.toDoubleOrNull(),
                fixQuality = fields.getOrNull(8)?.trim()?.let { parseFixLabel(it) } ?: 0,
                numSatellites = fields.getOrNull(9)?.trim()?.toIntOrNull() ?: 0,
                hdop = fields.getOrNull(10)?.trim()?.toDoubleOrNull(),
                averagingSeconds = fields.getOrNull(11)?.trim()?.toIntOrNull() ?: 0,
                timestamp = fields.getOrNull(12)?.trim()?.let { parseTimestamp(it) }
                    ?: System.currentTimeMillis(),
                remarks = fields.getOrNull(13)?.trim() ?: "",
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Reverse of [CsvExporter.fixLabel]. */
    private fun parseFixLabel(label: String): Int = when (label.uppercase()) {
        "RTK_FIX" -> 4
        "RTK_FLOAT" -> 5
        "DGPS" -> 2
        "GPS" -> 1
        "NONE" -> 0
        else -> label.toIntOrNull() ?: 0
    }

    private fun parseTimestamp(value: String): Long? =
        try {
            dateFormat.parse(value)?.time
        } catch (_: Exception) {
            null
        }

    /**
     * Splits a CSV line respecting quoted fields (handles commas and escaped
     * double-quotes inside quoted values).
     */
    private fun parseCsvFields(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++ // skip escaped quote
                    } else {
                        inQuotes = false
                    }
                }
                c == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }
}
