package org.opentopo.app

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.opentopo.app.db.AppDatabase
import org.opentopo.app.gnss.BluetoothGnssService
import org.opentopo.app.gnss.GnssState
import org.opentopo.app.gnss.UsbGnssService
import org.opentopo.app.ntrip.NtripClient
import org.opentopo.app.survey.Stakeout
import org.opentopo.app.survey.SurveyManager
import org.opentopo.app.ui.MainMapScreen
import org.opentopo.app.ui.theme.OpenTopoTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val ACTION_USB_PERMISSION = "org.opentopo.app.USB_PERMISSION"
    }

    private val gnssState = GnssState()
    private lateinit var bluetoothService: BluetoothGnssService
    private lateinit var usbService: UsbGnssService
    private lateinit var ntripClient: NtripClient
    private lateinit var db: AppDatabase
    private var surveyManager: SurveyManager? = null
    private var stakeout: Stakeout? = null

    /** Incremented on USB attach/detach to trigger recomposition of device list. */
    private val _usbDeviceVersion = MutableStateFlow(0)
    val usbDeviceVersion: StateFlow<Int> = _usbDeviceVersion

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* UI reacts via state */ }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    _usbDeviceVersion.value++
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    _usbDeviceVersion.value++
                }
                ACTION_USB_PERMISSION -> {
                    _usbDeviceVersion.value++
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        db = AppDatabase.getInstance(this)
        bluetoothService = BluetoothGnssService(this, gnssState)
        usbService = UsbGnssService(this, gnssState)

        // NTRIP RTCM data flows to whichever transport is connected
        ntripClient = NtripClient { rtcmData ->
            bluetoothService.write(rtcmData)
            usbService.write(rtcmData)
        }

        // Initialize transform-dependent services
        try {
            val deStream = assets.open("dE_2km_V1-0.grd")
            val dnStream = assets.open("dN_2km_V1-0.grd")
            surveyManager = SurveyManager(db, gnssState, deStream, dnStream)

            val deStream2 = assets.open("dE_2km_V1-0.grd")
            val dnStream2 = assets.open("dN_2km_V1-0.grd")
            stakeout = Stakeout(gnssState, deStream2, dnStream2)
        } catch (_: Exception) {
            // Grid files missing — transform features disabled
        }

        requestPermissionsIfNeeded()
        registerUsbReceiver()

        setContent {
            OpenTopoTheme {
                MainMapScreen(
                    gnssState = gnssState,
                    bluetoothService = bluetoothService,
                    usbService = usbService,
                    ntripClient = ntripClient,
                    db = db,
                    surveyManager = surveyManager,
                    stakeout = stakeout,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Handle USB device attached via intent (app launched by USB plug)
        handleUsbIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleUsbIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        ntripClient.disconnect()
        bluetoothService.disconnect()
        usbService.destroy()
        try {
            unregisterReceiver(usbReceiver)
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun handleUsbIntent(intent: Intent) {
        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            _usbDeviceVersion.value++
        }
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }
    }

    /** Force a refresh of the USB device list. */
    fun refreshUsbDevices() {
        _usbDeviceVersion.value++
    }

    /** Request USB permission for a specific device. */
    fun requestUsbPermission(device: UsbDevice) {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        if (!usbManager.hasPermission(device)) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_MUTABLE
            else
                0
            val permIntent = PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_USB_PERMISSION), flags
            )
            usbManager.requestPermission(device, permIntent)
        }
    }

    private fun requestPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) needed.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
            ) needed.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}
