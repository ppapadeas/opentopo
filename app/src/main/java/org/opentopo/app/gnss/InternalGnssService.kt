package org.opentopo.app.gnss

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Internal (device built-in) GPS connection.
 *
 * Uses Android's LocationManager to receive NMEA sentences from
 * the device's internal GNSS chipset. Follows the same data flow
 * as BluetoothGnssService and UsbGnssService: NMEA -> NmeaParser -> GnssState.
 */
class InternalGnssService(
    private val context: Context,
    private val gnssState: GnssState,
) {
    companion object {
        private const val TAG = "InternalGnss"
    }

    private val parser = NmeaParser(gnssState)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var nmeaListener: OnNmeaMessageListener? = null
    private var locationListener: LocationListener? = null
    private var isConnected = false

    /** Check if location permission is granted. */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Start receiving NMEA from the internal GPS. */
    @SuppressLint("MissingPermission")
    fun connect() {
        if (isConnected) return

        if (!hasPermission()) {
            Log.e(TAG, "ACCESS_FINE_LOCATION permission not granted")
            gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
            return
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.e(TAG, "GPS provider is disabled")
            gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
            return
        }

        disconnect()
        gnssState.setConnectionStatus(ConnectionStatus.CONNECTING)
        Log.d(TAG, "connecting to internal GPS")

        try {
            val handler = Handler(Looper.getMainLooper())

            // Register NMEA listener
            val nmea = OnNmeaMessageListener { message, _ ->
                if (message.startsWith("$")) {
                    parser.parseLine(message.trimEnd('\r', '\n'))
                }
            }
            nmeaListener = nmea
            locationManager.addNmeaListener(nmea, handler)

            // Request location updates to keep the GPS chipset active
            val locListener = LocationListener { /* we use NMEA instead */ }
            locationListener = locListener
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                locListener,
                Looper.getMainLooper(),
            )

            isConnected = true
            gnssState.setConnectionStatus(ConnectionStatus.CONNECTED)
            Log.d(TAG, "connected, NMEA listener registered")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
        } catch (e: Exception) {
            Log.e(TAG, "connect failed: ${e.message}")
            gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
        }
    }

    /** Stop receiving NMEA from the internal GPS. */
    fun disconnect() {
        nmeaListener?.let {
            try { locationManager.removeNmeaListener(it) } catch (_: Exception) {}
        }
        locationListener?.let {
            try { locationManager.removeUpdates(it) } catch (_: Exception) {}
        }
        nmeaListener = null
        locationListener = null
        isConnected = false
        gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
    }

    /** No-op — cannot send RTCM corrections to internal GPS. */
    fun write(data: ByteArray) {
        // Internal GPS does not accept external correction data
    }
}
