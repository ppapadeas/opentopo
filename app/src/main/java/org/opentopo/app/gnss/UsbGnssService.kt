package org.opentopo.app.gnss

import android.content.Context
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * USB-OTG serial connection to a GNSS receiver.
 *
 * Uses usb-serial-for-android library to communicate with
 * CDC/ACM or FTDI USB serial devices.
 */
class UsbGnssService(
    private val context: Context,
    private val gnssState: GnssState,
) {
    companion object {
        private const val DEFAULT_BAUD_RATE = 115200
        private const val READ_BUFFER_SIZE = 4096
        private const val READ_TIMEOUT_MS = 100
    }

    private var port: UsbSerialPort? = null
    private var readerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val parser = NmeaParser(gnssState)

    /** List available USB serial devices. */
    fun getAvailableDevices(): List<UsbSerialDriver> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
    }

    /** Connect to a USB serial device and start reading NMEA. */
    fun connect(driver: UsbSerialDriver, baudRate: Int = DEFAULT_BAUD_RATE) {
        disconnect()
        gnssState.setConnectionStatus(ConnectionStatus.CONNECTING)

        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
            return
        }

        val usbPort = driver.ports[0]
        try {
            usbPort.open(connection)
            usbPort.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            port = usbPort
            gnssState.setConnectionStatus(ConnectionStatus.CONNECTED)

            readerJob = scope.launch {
                readLoop(usbPort)
            }
        } catch (e: IOException) {
            gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
        }
    }

    /** Disconnect from the current device. */
    fun disconnect() {
        readerJob?.cancel()
        readerJob = null
        try {
            port?.close()
        } catch (_: IOException) {
        }
        port = null
        gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
    }

    /** Write bytes to the connected device (e.g., RTCM3 corrections). */
    fun write(data: ByteArray) {
        try {
            port?.write(data, READ_TIMEOUT_MS)
        } catch (_: IOException) {
        }
    }

    private suspend fun readLoop(usbPort: UsbSerialPort) {
        val buffer = ByteArray(READ_BUFFER_SIZE)
        val scope = kotlinx.coroutines.coroutineScope { this }
        while (scope.isActive) {
            try {
                val bytesRead = usbPort.read(buffer, READ_TIMEOUT_MS)
                if (bytesRead > 0) {
                    parser.feed(buffer, 0, bytesRead)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                break
            }
        }
        gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
    }
}
