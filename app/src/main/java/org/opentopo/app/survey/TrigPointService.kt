package org.opentopo.app.survey

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches nearby trigonometric points from the vathra.xyz API.
 *
 * GYS (Hellenic Army Geographical Service) trigonometric points are
 * Greek geodetic benchmarks with known EGSA87 coordinates.
 */
class TrigPointService {

    companion object {
        private const val BASE_URL = "https://vathra.xyz/api/trigpoints/nearby"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000
    }

    /**
     * Fetch trig points near the given WGS84 position.
     *
     * @param lat WGS84 latitude
     * @param lon WGS84 longitude
     * @param radiusM search radius in metres
     * @return list of nearby trig points, sorted by distance ascending
     */
    suspend fun getNearby(lat: Double, lon: Double, radiusM: Int): List<TrigPoint> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL?lat=$lat&lon=$lon&radius=$radiusM&limit=10")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "OpenTopo-Android")
                }
                try {
                    if (conn.responseCode != 200) return@withContext emptyList()
                    val body = conn.inputStream.bufferedReader().readText()
                    parseTrigPoints(body)
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

    private fun parseTrigPoints(json: String): List<TrigPoint> {
        val result = mutableListOf<TrigPoint>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            result += TrigPoint(
                gysId = obj.optString("gys_id", ""),
                name = obj.optString("name", null),
                latitude = obj.optDouble("lat", 0.0),
                longitude = obj.optDouble("lon", 0.0),
                egsa87E = obj.optDouble("egsa87_e").takeIf { !it.isNaN() },
                egsa87N = obj.optDouble("egsa87_n").takeIf { !it.isNaN() },
                elevation = obj.optDouble("elevation").takeIf { !it.isNaN() },
                status = obj.optString("status", null),
                sheet = obj.optString("sheet", null),
                distanceM = obj.optDouble("distance_m").takeIf { !it.isNaN() },
            )
        }
        return result
    }
}

data class TrigPoint(
    val gysId: String,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val egsa87E: Double?,
    val egsa87N: Double?,
    val elevation: Double?,
    val status: String?,       // "OK", "DAMAGED", "DESTROYED", "MISSING"
    val sheet: String?,        // GYS map sheet reference
    val distanceM: Double?,    // distance from query point
)
