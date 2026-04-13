package org.opentopo.app.survey

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches nearby trigonometric points from the api.vathra.xyz API.
 *
 * GYS (Hellenic Army Geographical Service) trigonometric points are
 * Greek geodetic benchmarks with known coordinates.
 */
class TrigPointService {

    companion object {
        private const val BASE_URL = "https://api.vathra.xyz/api/points/nearby"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000
    }

    /**
     * Fetch trig points near the given WGS84 position.
     *
     * @param lat WGS84 latitude
     * @param lon WGS84 longitude
     * @param radiusM search radius in metres (clamped to API max of 20000)
     * @return list of nearby trig points, sorted by distance ascending
     */
    suspend fun getNearby(lat: Double, lon: Double, radiusM: Int): List<TrigPoint> =
        withContext(Dispatchers.IO) {
            try {
                val clampedRadius = radiusM.coerceIn(100, 20_000)
                val url = URL("$BASE_URL?lat=$lat&lon=$lon&radius=$clampedRadius")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "OpenTopo-Android")
                }
                try {
                    if (conn.responseCode != 200) {
                        Log.w("TrigPointService", "API returned ${conn.responseCode}")
                        return@withContext emptyList()
                    }
                    val body = conn.inputStream.bufferedReader().readText()
                    parseTrigPoints(body)
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.e("TrigPointService", "Failed to fetch nearby trig points", e)
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
                elevation = obj.optDouble("elevation", Double.NaN).takeIf { !it.isNaN() },
                status = obj.optString("status", null),
                pointOrder = obj.optInt("point_order", 0),
                distanceM = obj.optDouble("distance_meters", Double.NaN).takeIf { !it.isNaN() },
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
    val elevation: Double?,
    val status: String?,       // "OK", "DAMAGED", "DESTROYED", "MISSING", "UNKNOWN"
    val pointOrder: Int,       // geodetic order (I, II, III, IV)
    val distanceM: Double?,    // distance from query point
)
