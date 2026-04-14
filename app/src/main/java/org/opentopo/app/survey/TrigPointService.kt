package org.opentopo.app.survey

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.opentopo.app.db.TrigPointCacheDao
import org.opentopo.app.db.TrigPointCacheEntity
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches nearby trigonometric points from the api.vathra.xyz API
 * with offline cache fallback via Room.
 */
class TrigPointService(
    private val cacheDao: TrigPointCacheDao? = null,
) {

    companion object {
        private const val BASE_URL = "https://api.vathra.xyz/api/points/nearby"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000
    }

    /**
     * Fetch trig points near the given WGS84 position.
     * Tries API first, falls back to offline cache on failure.
     */
    suspend fun getNearby(lat: Double, lon: Double, radiusM: Int): List<TrigPoint> {
        // Try API first
        val apiResult = fetchFromApi(lat, lon, radiusM)
        if (apiResult.isNotEmpty()) {
            // Cache results for offline use
            cacheDao?.let { dao ->
                try {
                    dao.insertAll(apiResult.map { TrigPointCacheEntity.from(it) })
                } catch (e: Exception) {
                    Log.w("TrigPointService", "Failed to cache trig points", e)
                }
            }
            return apiResult
        }

        // Fall back to offline cache
        return getFromCache(lat, lon, radiusM)
    }

    private suspend fun fetchFromApi(lat: Double, lon: Double, radiusM: Int): List<TrigPoint> =
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

    private suspend fun getFromCache(lat: Double, lon: Double, radiusM: Int): List<TrigPoint> {
        val dao = cacheDao ?: return emptyList()
        return try {
            // Convert radius to approximate degree range
            val degRange = radiusM / 111_000.0
            dao.getNearby(lat, lon, degRange).map { it.toTrigPoint() }
        } catch (e: Exception) {
            Log.w("TrigPointService", "Failed to read cache", e)
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
                egsa87Easting = obj.optDouble("egsa87_x", Double.NaN).takeIf { !it.isNaN() },
                egsa87Northing = obj.optDouble("egsa87_y", Double.NaN).takeIf { !it.isNaN() },
                egsa87Z = obj.optDouble("egsa87_z", Double.NaN).takeIf { !it.isNaN() },
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
    val latitude: Double,       // WGS84 (EPSG:4326)
    val longitude: Double,      // WGS84 (EPSG:4326)
    val elevation: Double?,     // orthometric height (Greek vertical datum, from leveling)
    val egsa87Easting: Double?, // published EGSA87 Easting (EPSG:2100)
    val egsa87Northing: Double?, // published EGSA87 Northing (EPSG:2100)
    val egsa87Z: Double?,       // GGRS87 ellipsoidal height
    val status: String?,        // "OK", "DAMAGED", "DESTROYED", "MISSING", "UNKNOWN"
    val pointOrder: Int,        // geodetic order (I, II, III, IV)
    val distanceM: Double?,     // distance from query point
)
