package org.opentopo.app.gnss

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reactive GNSS state holder.
 *
 * Consumes parsed NMEA data and exposes the current state via StateFlows.
 * Thread-safe — can be updated from the GNSS reader thread.
 */
class GnssState : NmeaListener {

    private val _position = MutableStateFlow(PositionState())
    val position: StateFlow<PositionState> = _position.asStateFlow()

    private val _accuracy = MutableStateFlow(AccuracyState())
    val accuracy: StateFlow<AccuracyState> = _accuracy.asStateFlow()

    private val _satellites = MutableStateFlow(SatelliteState())
    val satellites: StateFlow<SatelliteState> = _satellites.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    // Accumulate GSV messages across a sequence
    private val gsvAccumulator = mutableMapOf<Constellation, MutableList<SatelliteInfo>>()
    private var gsvExpectedMessages = mutableMapOf<Constellation, Int>()
    private var gsvReceivedMessages = mutableMapOf<Constellation, Int>()

    fun setConnectionStatus(status: ConnectionStatus) {
        _connectionStatus.value = status
    }

    /** Last raw GGA sentence for NTRIP VRS forwarding. */
    @Volatile
    var lastRawGga: String? = null
        private set

    override fun onRawGga(sentence: String) {
        lastRawGga = sentence
    }

    override fun onGga(data: GgaData) {
        val lat = data.latitude ?: return
        val lon = data.longitude ?: return
        _position.value = PositionState(
            latitude = lat,
            longitude = lon,
            altitude = data.altitude,
            geoidSeparation = data.geoidSeparation,
            fixQuality = data.quality,
            fixDescription = data.fixDescription,
            numSatellites = data.numSatellites,
            hdop = data.hdop,
            time = data.time,
            hasFix = data.quality > 0,
        )
    }

    override fun onRmc(data: RmcData) {
        if (data.status != 'A') return
        val lat = data.latitude
        val lon = data.longitude
        val current = _position.value
        _position.value = if (lat != null && lon != null) {
            // RMC has valid position — update coordinates if GGA hasn't set them yet
            current.copy(
                latitude = lat,
                longitude = lon,
                speedKnots = data.speedKnots,
                courseTrue = data.courseTrue,
                date = data.date,
                hasFix = current.hasFix || true,
                fixQuality = if (current.fixQuality == 0) 1 else current.fixQuality,
                fixDescription = if (current.fixQuality == 0) "GPS" else current.fixDescription,
            )
        } else {
            current.copy(
                speedKnots = data.speedKnots,
                courseTrue = data.courseTrue,
                date = data.date,
            )
        }
    }

    override fun onGsa(data: GsaData) {
        _accuracy.value = _accuracy.value.copy(
            fixType = data.fixType,
            pdop = data.pdop,
            hdop = data.hdop,
            vdop = data.vdop,
            activeSatellitePrns = data.satellitePrns,
        )
    }

    override fun onGsv(data: GsvData) {
        val constellation = data.constellation
        if (data.messageNumber == 1) {
            gsvAccumulator[constellation] = mutableListOf()
            gsvExpectedMessages[constellation] = data.totalMessages
            gsvReceivedMessages[constellation] = 0
        }
        gsvAccumulator[constellation]?.addAll(data.satellites)
        gsvReceivedMessages[constellation] = (gsvReceivedMessages[constellation] ?: 0) + 1

        // When we've received all messages for this constellation, update state
        if (gsvReceivedMessages[constellation] == gsvExpectedMessages[constellation]) {
            val allSatellites = gsvAccumulator.values.flatten()
            _satellites.value = SatelliteState(
                satellites = allSatellites,
                totalInView = allSatellites.size,
            )
        }
    }

    override fun onGst(data: GstData) {
        _accuracy.value = _accuracy.value.copy(
            latitudeErrorM = data.latitudeErrorM,
            longitudeErrorM = data.longitudeErrorM,
            altitudeErrorM = data.altitudeErrorM,
        )
    }
}

data class PositionState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double? = null,
    val geoidSeparation: Double? = null,
    val fixQuality: Int = 0,
    val fixDescription: String = "No fix",
    val numSatellites: Int = 0,
    val hdop: Double? = null,
    val time: String = "",
    val date: String = "",
    val speedKnots: Double? = null,
    val courseTrue: Double? = null,
    val hasFix: Boolean = false,
)

data class AccuracyState(
    val fixType: Int = 1,           // 1=no fix, 2=2D, 3=3D
    val pdop: Double? = null,
    val hdop: Double? = null,
    val vdop: Double? = null,
    val activeSatellitePrns: List<Int> = emptyList(),
    val latitudeErrorM: Double? = null,
    val longitudeErrorM: Double? = null,
    val altitudeErrorM: Double? = null,
) {
    /** Estimated horizontal accuracy from GST (1-sigma), or from HDOP approximation. */
    val horizontalAccuracyM: Double?
        get() {
            val latErr = latitudeErrorM
            val lonErr = longitudeErrorM
            if (latErr != null && lonErr != null) {
                return kotlin.math.sqrt(latErr * latErr + lonErr * lonErr)
            }
            return hdop?.times(2.5) // rough HDOP-to-accuracy approximation
        }
}

data class SatelliteState(
    val satellites: List<SatelliteInfo> = emptyList(),
    val totalInView: Int = 0,
) {
    val byConstellation: Map<Constellation, List<SatelliteInfo>>
        get() = satellites.groupBy { it.constellation }
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
}
