package org.opentopo.app.gnss

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException

/**
 * USB-OTG serial connection to a GNSS receiver.
 *
 * Uses usb-serial-for-android's [SerialInputOutputManager] for
 * reliable event-driven reading from CDC/ACM, FTDI, CP210x, CH340,
 * and PL2303 USB serial devices.
 */
class UsbGnssService(
    private val context: Context,
    private val gnssState: GnssState,
) : SerialInputOutputManager.Listener {

    companion object {
        private const val TAG = "UsbGnss"
        private const val ACTION_USB_PERMISSION = "org.opentopo.app.USB_PERMISSION"
        private const val DEFAULT_BAUD_RATE = 115200
        private const val READ_QUEUE_BUFFERS = 8
    }

    private var port: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null
    private val parser = NmeaParser(gnssState)

    /** Pending driver waiting for USB permission grant. */
    private var pendingDriver: UsbSerialDriver? = null
    private var pendingBaudRate: Int = DEFAULT_BAUD_RATE

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (granted) {
                    pendingDriver?.let { connectInternal(it, pendingBaudRate) }
                } else {
                    gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
                }
                pendingDriver = null
            }
        }
    }

    init {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(permissionReceiver, filter)
        }
    }

    /** List available USB serial devices. */
    fun getAvailableDevices(): List<UsbSerialDriver> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
    }

    /** Connect to a USB serial device. Requests permission if needed. */
    fun connect(driver: UsbSerialDriver, baudRate: Int = DEFAULT_BAUD_RATE) {
        disconnect()
        gnssState.setConnectionStatus(ConnectionStatus.CONNECTING)

        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        if (usbManager.hasPermission(driver.device)) {
            connectInternal(driver, baudRate)
        } else {
            pendingDriver = driver
            pendingBaudRate = baudRate
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_MUTABLE
            else
                0
            val intent = Intent(ACTION_USB_PERMISSION).apply {
                setPackage(context.packageName)
            }
            val permIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
            usbManager.requestPermission(driver.device, permIntent)
        }
    }

    private fun connectInternal(driver: UsbSerialDriver, baudRate: Int) {
        Log.d(TAG, "connectInternal: ${driver.device.productName}, baud=$baudRate")
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val connection = try {
            usbManager.openDevice(driver.device)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException opening device", e)
            gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
            return
        }
        if (connection == null) {
            Log.e(TAG, "openDevice returned null")
            gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
            return
        }

        if (driver.ports.isEmpty()) {
            Log.e(TAG, "driver has no ports")
            gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
            return
        }

        val usbPort = driver.ports[0]
        try {
            usbPort.open(connection)
            usbPort.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            port = usbPort

            // Event-driven I/O manager — handles USB buffering reliably
            val manager = SerialInputOutputManager(usbPort, this)
            manager.readTimeout = 0 // blocking read — most reliable for continuous streams
            manager.start()
            ioManager = manager

            gnssState.setConnectionStatus(ConnectionStatus.CONNECTED)
            Log.d(TAG, "connected, SerialInputOutputManager started")
        } catch (e: IOException) {
            Log.e(TAG, "IOException during connect", e)
            gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
        }
    }

    // ── SerialInputOutputManager.Listener ──

    override fun onNewData(data: ByteArray) {
        parser.feed(data, 0, data.size)
    }

    override fun onRunError(e: Exception) {
        Log.e(TAG, "SerialInputOutputManager error", e)
        disconnect()
    }

    /** Disconnect from the current device. */
    fun disconnect() {
        ioManager?.stop()
        ioManager = null
        pendingDriver = null
        try {
            port?.close()
        } catch (_: IOException) {
        }
        port = null
        gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
    }

    /** Clean up the permission receiver. Call from Activity.onDestroy(). */
    fun destroy() {
        disconnect()
        try {
            context.unregisterReceiver(permissionReceiver)
        } catch (_: IllegalArgumentException) {
        }
    }

    /** Write bytes to the connected device (e.g., RTCM3 corrections). */
    fun write(data: ByteArray) {
        try {
            ioManager?.writeAsync(data)
        } catch (_: Exception) {
        }
    }
}
