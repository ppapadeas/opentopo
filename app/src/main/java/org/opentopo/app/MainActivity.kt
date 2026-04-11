package org.opentopo.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
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

    private val gnssState = GnssState()
    private lateinit var bluetoothService: BluetoothGnssService
    private lateinit var usbService: UsbGnssService
    private lateinit var ntripClient: NtripClient
    private lateinit var db: AppDatabase
    private var surveyManager: SurveyManager? = null
    private var stakeout: Stakeout? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* UI reacts via state */ }

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
    }

    override fun onDestroy() {
        super.onDestroy()
        ntripClient.disconnect()
        bluetoothService.disconnect()
        usbService.disconnect()
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
