package org.opentopo.app.gnss

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Bluetooth Classic SPP connection to a GNSS receiver.
 *
 * Connects to a paired Bluetooth device, reads the NMEA stream,
 * and feeds it to an [NmeaParser] which updates [GnssState].
 */
class BluetoothGnssService(
    private val context: Context,
    private val gnssState: GnssState,
) {
    companion object {
        /** Standard SPP UUID for serial port profile. */
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val READ_BUFFER_SIZE = 4096
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var readerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val parser = NmeaParser(gnssState)

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    /** List paired Bluetooth devices. Requires BLUETOOTH_CONNECT permission. */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    /** Connect to a paired Bluetooth device and start reading NMEA. */
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        disconnect()
        gnssState.setConnectionStatus(ConnectionStatus.CONNECTING)

        readerJob = scope.launch {
            try {
                val btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket = btSocket

                // Cancel discovery if running (saves power, speeds up connect)
                bluetoothAdapter?.cancelDiscovery()

                btSocket.connect()
                outputStream = btSocket.outputStream
                _connectedDevice.value = device
                gnssState.setConnectionStatus(ConnectionStatus.CONNECTED)

                readLoop(btSocket.inputStream)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
                _connectedDevice.value = null
            }
        }
    }

    /** Disconnect from the current device. */
    fun disconnect() {
        readerJob?.cancel()
        readerJob = null
        try {
            socket?.close()
        } catch (_: IOException) {
        }
        socket = null
        outputStream = null
        _connectedDevice.value = null
        gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
    }

    /** Write bytes to the connected device (e.g., RTCM3 corrections from NTRIP). */
    fun write(data: ByteArray) {
        try {
            outputStream?.write(data)
            outputStream?.flush()
        } catch (_: IOException) {
        }
    }

    private suspend fun readLoop(inputStream: InputStream) {
        val buffer = ByteArray(READ_BUFFER_SIZE)
        try {
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead > 0) {
                    parser.feed(buffer, 0, bytesRead)
                } else if (bytesRead == -1) {
                    break
                }
            }
        } catch (_: kotlinx.coroutines.CancellationException) {
            // Normal disconnect
        } catch (_: IOException) {
            // Device disconnected
        }
        gnssState.setConnectionStatus(ConnectionStatus.DISCONNECTED)
        _connectedDevice.value = null
    }

    val isBluetoothAvailable: Boolean
        get() = bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true
}
