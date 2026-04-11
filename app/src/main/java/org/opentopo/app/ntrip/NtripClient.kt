package org.opentopo.app.ntrip

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.util.Base64

/**
 * NTRIP v1 client using raw sockets for bidirectional communication.
 *
 * Connects to an NTRIP caster, sends GGA for VRS generation,
 * receives RTCM3 correction data, and forwards it via [onRtcmData] callback.
 *
 * Uses raw TCP sockets instead of HttpURLConnection because NTRIP
 * requires sending GGA data back to the caster on the same connection,
 * and HttpURLConnection's doOutput converts GET to POST.
 */
class NtripClient(
    private val onRtcmData: (ByteArray) -> Unit,
) {
    companion object {
        private const val TAG = "NtripClient"
        private const val READ_BUFFER_SIZE = 4096
        private const val RECONNECT_BASE_DELAY_MS = 2000L
        private const val RECONNECT_MAX_DELAY_MS = 60000L
        private const val USER_AGENT = "NTRIP OpenTopo/0.1"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var connectionJob: Job? = null
    private var ggaJob: Job? = null
    private var socket: Socket? = null
    private var socketOutput: OutputStream? = null

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
                Log.e(TAG, "fetchSourcetable failed", e)
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

        while (currentCoroutineContext().isActive) {
            try {
                doConnect(config)
                reconnectDelay = RECONNECT_BASE_DELAY_MS
                startGgaForwarding(config)
                readRtcmStream()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "connection error", e)
                _state.value = _state.value.copy(
                    status = NtripStatus.RECONNECTING,
                    error = e.message,
                )
            }

            closeConnection()
            ggaJob?.cancel()

            if (!currentCoroutineContext().isActive) break

            delay(reconnectDelay)
            reconnectDelay = (reconnectDelay * 2).coerceAtMost(RECONNECT_MAX_DELAY_MS)
        }
    }

    /**
     * Connect via raw TCP socket with NTRIP v1 HTTP request.
     * This allows bidirectional communication (send GGA, receive RTCM).
     */
    private fun doConnect(config: NtripConfig) {
        Log.d(TAG, "connecting to ${config.host}:${config.port}/${config.mountpoint}")

        val sock = Socket(config.host, config.port)
        sock.soTimeout = 30_000
        socket = sock

        val output = sock.getOutputStream()
        socketOutput = output

        // Send NTRIP v1 GET request
        val request = buildString {
            append("GET /${config.mountpoint} HTTP/1.1\r\n")
            append("Host: ${config.host}\r\n")
            append("User-Agent: $USER_AGENT\r\n")
            append("Ntrip-Version: Ntrip/2.0\r\n")
            append("Accept: */*\r\n")
            if (config.username.isNotBlank()) {
                val credentials = "${config.username}:${config.password}"
                val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray())
                append("Authorization: Basic $encoded\r\n")
            }
            append("\r\n")
        }
        output.write(request.toByteArray())
        output.flush()

        // Read HTTP response status line
        val input = sock.getInputStream()
        val responseLine = readLine(input)
        Log.d(TAG, "response: $responseLine")

        if (responseLine == null || !responseLine.contains("200")) {
            throw IOException("NTRIP server returned: $responseLine")
        }

        // Skip remaining HTTP headers
        while (true) {
            val header = readLine(input) ?: break
            if (header.isBlank()) break
        }

        // Send initial GGA immediately if available
        if (config.sendGga) {
            val gga = lastGga
            if (gga != null) {
                try {
                    output.write("$gga\r\n".toByteArray())
                    output.flush()
                    Log.d(TAG, "sent initial GGA")
                } catch (_: IOException) {
                }
            }
        }

        _state.value = _state.value.copy(
            status = NtripStatus.CONNECTED,
            mountpoint = config.mountpoint,
            error = null,
        )
    }

    /** Read a single line from an InputStream (up to \n). */
    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1) return if (sb.isEmpty()) null else sb.toString()
            val c = b.toChar()
            if (c == '\n') return sb.toString().trimEnd('\r')
            sb.append(c)
        }
    }

    private suspend fun readRtcmStream() {
        val sock = socket ?: return
        val input = BufferedInputStream(sock.getInputStream(), READ_BUFFER_SIZE)
        val buffer = ByteArray(READ_BUFFER_SIZE)
        var totalBytes = 0L
        val startTime = System.currentTimeMillis()

        while (currentCoroutineContext().isActive) {
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
                val output = socketOutput
                if (gga != null && output != null) {
                    try {
                        output.write("$gga\r\n".toByteArray())
                        output.flush()
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
            socketOutput?.close()
        } catch (_: IOException) {
        }
        try {
            socket?.close()
        } catch (_: IOException) {
        }
        socketOutput = null
        socket = null
    }

    // ── Sourcetable parsing (uses HttpURLConnection — simple GET, no bidirectional) ──

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
