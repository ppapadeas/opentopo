package org.opentopo.app.gnss

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Looper
import androidx.annotation.RequiresPermission

/**
 * Internal (device built-in) GPS connection.
 *
 * Uses Android's LocationManager to receive NMEA sentences from
 * the device's internal GNSS chipset. Follows the same data flow
 * as BluetoothGnssService and UsbGnssService: NMEA -> NmeaParser -> GnssState.
 *
 * Limitations:
 * - Cannot receive RTCM corrections (write() is a no-op)
 * - Accuracy depends on device hardware (typically 2-5m, no RTK)
 * - Useful for testing, quick field checks, or as a fallback
 */
class InternalGnssService(
    private val context: Context,
    private val gnssState: GnssState,
) {
    private val parser = NmeaParser(gnssState)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var nmeaListener: OnNmeaMessageListener? = null
    private var isConnected = false

    /** Start receiving NMEA from the internal GPS. */
    @SuppressLint("MissingPermission")
    fun connect() {
        if (isConnected) return
        disconnect()
        gnssState.setConnectionStatus(ConnectionStatus.CONNECTING)

        try {
            // Register NMEA listener — receives raw sentences like $GPGGA, $GPRMC, etc.
            val listener = OnNmeaMessageListener { message, _ ->
                if (message.startsWith("$")) {
                    parser.parseLine(message.trimEnd('\r', '\n'))
                }
            }
            nmeaListener = listener
            locationManager.addNmeaListener(listener, android.os.Handler(Looper.getMainLooper()))

            // Also request location updates to keep the GPS active.
            // Without this, some devices don't generate NMEA sentences.
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,  // 1 second interval
                0f,     // 0 meter minimum distance
                { /* location update — we use NMEA instead */ },
                Looper.getMainLooper(),
            )

            isConnected = true
            gnssState.setConnectionStatus(ConnectionStatus.CONNECTED)
        } catch (e: SecurityException) {
            gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
        } catch (e: Exception) {
            gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
        }
    }

    /** Stop receiving NMEA from the internal GPS. */
    fun disconnect() {
        nmeaListener?.let { locationManager.removeNmeaListener(it) }
        nmeaListener = null
        try {
            locationManager.removeUpdates { /* no-op listener */ }
        } catch (_: Exception) {
        }
        isConnected = false
        gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
    }

    /** No-op — cannot send RTCM corrections to internal GPS. */
    fun write(data: ByteArray) {
        // Internal GPS does not accept external correction data
    }
}
