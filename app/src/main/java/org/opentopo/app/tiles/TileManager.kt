package org.opentopo.app.tiles

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages offline PMTiles for the map basemap and contours.
 *
 * Downloads PMTiles from the vathra.xyz R2 bucket to app-private storage.
 * When local tiles exist, the style uses pmtiles://file:// URLs for
 * offline rendering via MapLibre's native PMTiles support.
 */
class TileManager(private val context: Context) {

    companion object {
        private const val TAG = "TileManager"
        private const val TILES_DIR = "tiles"

        // These URLs need to serve the raw PMTiles files.
        // Update when the vathra worker exposes direct file access,
        // or use direct R2 public URLs.
        const val GREECE_PMTILES_URL = "https://vathra-tiles.vathra.workers.dev/greece.pmtiles"
        const val CONTOURS_PMTILES_URL = "https://vathra-tiles.vathra.workers.dev/contours.pmtiles"

        const val GREECE_FILENAME = "greece.pmtiles"
        const val CONTOURS_FILENAME = "contours.pmtiles"
    }

    private val tilesDir = File(context.filesDir, TILES_DIR).also { it.mkdirs() }

    private val _downloadState = MutableStateFlow(DownloadState.IDLE)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    /** Check if local PMTiles exist for the basemap. */
    fun hasGreeceTiles(): Boolean = File(tilesDir, GREECE_FILENAME).exists()

    /** Check if local PMTiles exist for contours. */
    fun hasContourTiles(): Boolean = File(tilesDir, CONTOURS_FILENAME).exists()

    /** Get the local file path for MapLibre pmtiles:// protocol. */
    fun greecePmtilesUri(): String =
        "pmtiles://file://${File(tilesDir, GREECE_FILENAME).absolutePath}"

    /** Get the local contours file path. */
    fun contoursPmtilesUri(): String =
        "pmtiles://file://${File(tilesDir, CONTOURS_FILENAME).absolutePath}"

    /** Size of local basemap file in MB, or null if not downloaded. */
    fun greeceSizeMb(): Double? {
        val f = File(tilesDir, GREECE_FILENAME)
        return if (f.exists()) f.length() / (1024.0 * 1024.0) else null
    }

    /** Size of local contours file in MB, or null if not downloaded. */
    fun contoursSizeMb(): Double? {
        val f = File(tilesDir, CONTOURS_FILENAME)
        return if (f.exists()) f.length() / (1024.0 * 1024.0) else null
    }

    /** Delete all downloaded tiles. */
    fun deleteTiles() {
        File(tilesDir, GREECE_FILENAME).delete()
        File(tilesDir, CONTOURS_FILENAME).delete()
        Log.d(TAG, "deleted local tiles")
    }

    /** Download both PMTiles files with progress tracking. */
    suspend fun downloadTiles() {
        _downloadState.value = DownloadState.DOWNLOADING
        _downloadProgress.value = 0f

        try {
            // Download basemap (70% of progress)
            downloadFile(GREECE_PMTILES_URL, GREECE_FILENAME) { progress ->
                _downloadProgress.value = progress * 0.7f
            }

            // Download contours (30% of progress)
            downloadFile(CONTOURS_PMTILES_URL, CONTOURS_FILENAME) { progress ->
                _downloadProgress.value = 0.7f + progress * 0.3f
            }

            _downloadProgress.value = 1f
            _downloadState.value = DownloadState.COMPLETED
            Log.d(TAG, "download completed")
        } catch (e: Exception) {
            Log.e(TAG, "download failed: ${e.message}")
            _downloadState.value = DownloadState.FAILED
        }
    }

    private suspend fun downloadFile(
        urlStr: String,
        filename: String,
        onProgress: (Float) -> Unit,
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "downloading $urlStr")
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000

        val responseCode = conn.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            conn.disconnect()
            throw java.io.IOException("HTTP $responseCode for $urlStr")
        }

        val totalBytes = conn.contentLengthLong
        val tempFile = File(tilesDir, "$filename.tmp")
        val targetFile = File(tilesDir, filename)

        var downloadedBytes = 0L
        val buffer = ByteArray(8192)

        conn.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                while (true) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        onProgress(downloadedBytes.toFloat() / totalBytes)
                    }
                }
            }
        }
        conn.disconnect()

        // Atomic rename
        tempFile.renameTo(targetFile)
        Log.d(TAG, "saved $filename (${targetFile.length() / 1024 / 1024} MB)")
    }

    /**
     * Build a style JSON string using local PMTiles if available,
     * or fall back to remote tile URLs.
     */
    fun buildStyleJson(assetStyleJson: String): String {
        if (!hasGreeceTiles()) return assetStyleJson

        // Replace remote tile source URL with local pmtiles:// URI
        var style = assetStyleJson.replace(
            "\"url\": \"https://vathra-tiles.vathra.workers.dev/greece.json\"",
            "\"url\": \"${greecePmtilesUri()}\"",
        )

        if (hasContourTiles()) {
            style = style.replace(
                "\"url\": \"https://vathra-tiles.vathra.workers.dev/contours.json\"",
                "\"url\": \"${contoursPmtilesUri()}\"",
            )
        }

        return style
    }
}

enum class DownloadState {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED,
}
