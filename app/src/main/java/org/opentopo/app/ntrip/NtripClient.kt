package org.opentopo.app.ntrip

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * NTRIP v1/v2 client.
 *
 * Connects to an NTRIP caster, optionally sends GGA for VRS generation,
 * receives RTCM3 correction data, and forwards it via [onRtcmData] callback.
 */
class NtripClient(
    private val onRtcmData: (ByteArray) -> Unit,
) {
    companion object {
        private const val READ_BUFFER_SIZE = 4096
        private const val RECONNECT_BASE_DELAY_MS = 2000L
        private const val RECONNECT_MAX_DELAY_MS = 60000L
        private const val USER_AGENT = "NTRIP OpenTopo/0.1"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var connectionJob: Job? = null
    private var ggaJob: Job? = null
    private var connection: HttpURLConnection? = null
    private var outputStream: OutputStream? = null

    private val _state = MutableStateFlow(NtripState())
    val state: StateFlow<NtripState> = _state.asStateFlow()

    private var currentConfig: NtripConfig? = null
    private var lastGga: String? = null

    /** Update the GGA sentence to send to the caster for VRS. */
    fun updateGga(ggaSentence: String) {
        lastGga = ggaSentence
    }

    /** Fetch the sourcetable (list of mountpoints) from a caster. */
    fun fetchSourcetable(config: NtripConfig, callback: (Result<List<NtripMountpoint>>) -> Unit) {
        scope.launch {
            try {
                val result = doFetchSourcetable(config)
                callback(Result.success(result))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

    /** Connect to a caster/mountpoint and start receiving RTCM data. */
    fun connect(config: NtripConfig) {
        disconnect()
        currentConfig = config
        _state.value = NtripState(status = NtripStatus.CONNECTING)
        connectionJob = scope.launch { connectLoop(config) }
    }

    /** Disconnect from the caster. */
    fun disconnect() {
        ggaJob?.cancel()
        ggaJob = null
        connectionJob?.cancel()
        connectionJob = null
        closeConnection()
        currentConfig = null
        _state.value = NtripState(status = NtripStatus.DISCONNECTED)
    }

    // ── Connection loop with auto-reconnect ──

    private suspend fun connectLoop(config: NtripConfig) {
        var reconnectDelay = RECONNECT_BASE_DELAY_MS

        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            try {
                doConnect(config)
                reconnectDelay = RECONNECT_BASE_DELAY_MS // reset on success
                startGgaForwarding(config)
                readRtcmStream()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    status = NtripStatus.RECONNECTING,
                    error = e.message,
                )
            }

            closeConnection()
            ggaJob?.cancel()

            if (!kotlinx.coroutines.currentCoroutineContext().isActive) break

            delay(reconnectDelay)
            reconnectDelay = (reconnectDelay * 2).coerceAtMost(RECONNECT_MAX_DELAY_MS)
        }
    }

    private fun doConnect(config: NtripConfig) {
        val url = URL("http://${config.host}:${config.port}/${config.mountpoint}")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Ntrip-Version", "Ntrip/2.0")
            setRequestProperty("Accept", "*/*")
            if (config.username.isNotBlank()) {
                val credentials = "${config.username}:${config.password}"
                val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray())
                setRequestProperty("Authorization", "Basic $encoded")
            }
            connectTimeout = 10_000
            readTimeout = 30_000
            doInput = true
            // Do NOT set doOutput here — it converts GET to POST and casters reject it.
            // GGA is sent via the input stream's socket after connection is established.
        }

        connection = conn

        val responseCode = conn.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("NTRIP server returned HTTP $responseCode")
        }

        // Note: GGA forwarding via HttpURLConnection is not reliable —
        // doOutput=true converts GET to POST, which casters reject.
        // VRS casters that need GGA will require a raw socket implementation.
        outputStream = null

        _state.value = _state.value.copy(
            status = NtripStatus.CONNECTED,
            mountpoint = config.mountpoint,
            error = null,
        )
    }

    private suspend fun readRtcmStream() {
        val conn = connection ?: return
        val input = BufferedInputStream(conn.inputStream, READ_BUFFER_SIZE)
        val buffer = ByteArray(READ_BUFFER_SIZE)
        var totalBytes = 0L
        val startTime = System.currentTimeMillis()

        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            val bytesRead = input.read(buffer)
            if (bytesRead == -1) break
            if (bytesRead > 0) {
                totalBytes += bytesRead
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                val dataRate = if (elapsed > 0) (totalBytes / elapsed).toLong() else 0L

                onRtcmData(buffer.copyOf(bytesRead))

                _state.value = _state.value.copy(
                    bytesReceived = totalBytes,
                    dataRateBps = dataRate,
                    lastDataTime = System.currentTimeMillis(),
                )
            }
        }
    }

    private fun startGgaForwarding(config: NtripConfig) {
        if (!config.sendGga) return
        ggaJob?.cancel()
        ggaJob = scope.launch {
            while (isActive) {
                val gga = lastGga
                if (gga != null) {
                    try {
                        outputStream?.write("$gga\r\n".toByteArray())
                        outputStream?.flush()
                    } catch (_: IOException) {
                        // connection lost — readRtcmStream will handle reconnect
                    }
                }
                delay(config.ggaIntervalSeconds * 1000L)
            }
        }
    }

    private fun closeConnection() {
        try {
            outputStream?.close()
        } catch (_: IOException) {
        }
        try {
            connection?.disconnect()
        } catch (_: Exception) {
        }
        outputStream = null
        connection = null
    }

    // ── Sourcetable parsing ──

    private fun doFetchSourcetable(config: NtripConfig): List<NtripMountpoint> {
        val url = URL("http://${config.host}:${config.port}/")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Ntrip-Version", "Ntrip/2.0")
            if (config.username.isNotBlank()) {
                val credentials = "${config.username}:${config.password}"
                val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray())
                setRequestProperty("Authorization", "Basic $encoded")
            }
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        val response = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        return parseSourcetable(response)
    }

    private fun parseSourcetable(text: String): List<NtripMountpoint> {
        return text.lineSequence()
            .filter { it.startsWith("STR;") }
            .mapNotNull { line ->
                val fields = line.split(";")
                if (fields.size < 19) return@mapNotNull null
                NtripMountpoint(
                    name = fields[1],
                    identifier = fields[2],
                    format = fields[3],
                    formatDetails = fields[4],
                    carrier = fields[5].toIntOrNull() ?: 0,
                    navSystem = fields[6],
                    network = fields[7],
                    country = fields[8],
                    latitude = fields[9].toDoubleOrNull() ?: 0.0,
                    longitude = fields[10].toDoubleOrNull() ?: 0.0,
                    nmea = fields[11] == "1",
                    solution = fields[12] == "1",
                    generator = fields[13],
                    compression = fields[14],
                    authentication = fields[15],
                    fee = fields[16] == "Y",
                    bitrate = fields[17].toIntOrNull() ?: 0,
                )
            }
            .toList()
    }
}

// ── State ──

data class NtripState(
    val status: NtripStatus = NtripStatus.DISCONNECTED,
    val mountpoint: String = "",
    val bytesReceived: Long = 0,
    val dataRateBps: Long = 0,
    val lastDataTime: Long = 0,
    val error: String? = null,
) {
    /** Seconds since last RTCM data was received. */
    val ageOfCorrectionSeconds: Long
        get() = if (lastDataTime > 0) {
            (System.currentTimeMillis() - lastDataTime) / 1000
        } else {
            -1
        }

    val dataRateFormatted: String
        get() = when {
            dataRateBps > 1024 -> "%.1f KB/s".format(dataRateBps / 1024.0)
            else -> "$dataRateBps B/s"
        }
}

enum class NtripStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
}
