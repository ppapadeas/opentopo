package org.opentopo.app.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hoho.android.usbserial.driver.UsbSerialDriver
import kotlinx.coroutines.launch
import org.opentopo.app.gnss.BluetoothGnssService
import org.opentopo.app.gnss.ConnectionStatus
import org.opentopo.app.gnss.GnssState
import org.opentopo.app.gnss.UsbGnssService
import org.opentopo.app.ntrip.NtripClient
import org.opentopo.app.ntrip.NtripConfig
import org.opentopo.app.ntrip.NtripMountpoint
import org.opentopo.app.ntrip.NtripStatus
import org.opentopo.app.ui.components.ButtonGroup as OpenTopoButtonGroup
import org.opentopo.app.ui.components.ConstellationChip
import org.opentopo.app.ui.components.FixStatusPill
import org.opentopo.app.ui.components.InputRow
import org.opentopo.app.ui.components.SectionLabel
import org.opentopo.app.ui.components.TonalCard
import org.opentopo.app.ui.theme.CoordinateFont
import org.opentopo.app.ui.theme.LabelOverline
import org.opentopo.app.ui.theme.LocalSurveyColors
import org.opentopo.app.ui.theme.MonoDelta

// Hard-coded preset targets for the NTRIP caster selector. Order must match the
// labels passed to the OpenTopoButtonGroup below.
private data class NtripPreset(
    val label: String,
    val host: String,
    val port: String,
    val mount: String,
)

private val NtripPresets = listOf(
    NtripPreset("HEPOS", "rtk.hepos.gr", "2101", "RTCM32"),
    NtripPreset("CivilPOS", "civilpos.net", "2101", "RTCM3_NET"),
    NtripPreset("SmartNet", "rtk.smartnet.leica-geosystems.com", "2101", "iMAX_Eur"),
    NtripPreset("Custom", "", "", ""),
)
private const val NTRIP_CUSTOM_INDEX = 3

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectionPanel(
    gnssState: GnssState,
    bluetoothService: BluetoothGnssService,
    usbService: UsbGnssService,
    internalService: org.opentopo.app.gnss.InternalGnssService,
    ntripClient: NtripClient,
    modifier: Modifier = Modifier,
) {
    val surveyColors = LocalSurveyColors.current
    val connectionStatus by gnssState.connectionStatus.collectAsState()
    val position by gnssState.position.collectAsState()
    val accuracy by gnssState.accuracy.collectAsState()
    val satellites by gnssState.satellites.collectAsState()
    val ntripState by ntripClient.state.collectAsState()
    var connectionType by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(0.dp))

        // -- GNSS Receiver --
        SectionLabel("GNSS Receiver")
        TonalCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val isConnected = connectionStatus == ConnectionStatus.CONNECTED
                val isConnecting = connectionStatus == ConnectionStatus.CONNECTING

                // Show connection method selector only when disconnected
                if (!isConnected && !isConnecting) {
                    OpenTopoButtonGroup(
                        options = listOf("Bluetooth", "USB", "Internal"),
                        selectedIndex = connectionType,
                        onSelect = { connectionType = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (isConnected || isConnecting) {
                    // Show which connection method is active
                    val methodLabel = when (connectionType) {
                        0 -> "Bluetooth"
                        1 -> "USB"
                        2 -> "Internal GPS"
                        else -> "Connected"
                    }
                    val methodIcon = when (connectionType) {
                        0 -> Icons.Outlined.Bluetooth
                        1 -> Icons.Outlined.Usb
                        else -> Icons.Outlined.PhoneAndroid
                    }
                    // Connected / connecting state — emphasized with primaryContainer
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            // Top row: FixStatusPill + Check icon
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (isConnecting) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        ContainedLoadingIndicator(
                                            modifier = Modifier.size(24.dp),
                                        )
                                        Text(
                                            "Connecting",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                } else {
                                    val hAcc = accuracy.horizontalAccuracyM
                                    val extras = if (hAcc != null) {
                                        "%.2f m \u00b7 ${satellites.satellites.size} sats".format(hAcc)
                                    } else {
                                        "${satellites.satellites.size} sats"
                                    }
                                    FixStatusPill(
                                        fixQuality = position.fixQuality,
                                        extras = extras,
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }

                            if (isConnected && position.hasFix) {
                                Spacer(Modifier.height(10.dp))
                                // Mid row: device / connection name + path
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        methodIcon,
                                        null,
                                        Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Text(
                                        methodLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                Text(
                                    position.fixDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Spacer(Modifier.height(10.dp))
                                // Bottom row: horizontally-scrollable constellation chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    satellites.byConstellation.forEach { (c, s) ->
                                        ConstellationChip(c, s.size)
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                SurveyStatusRow(
                                    "H-Accuracy",
                                    accuracy.horizontalAccuracyM?.let {
                                        "%.3f m".format(it)
                                    } ?: "\u2014",
                                    valueColor = surveyColors.accuracyColor(
                                        accuracy.horizontalAccuracyM,
                                    ),
                                )
                                SurveyStatusRow(
                                    "V-Accuracy",
                                    accuracy.altitudeErrorM?.let {
                                        "%.3f m".format(it)
                                    } ?: "\u2014",
                                )
                                SurveyStatusRow(
                                    "HDOP",
                                    accuracy.hdop?.let { "%.1f".format(it) } ?: "\u2014",
                                )

                                // Skyplot
                                if (satellites.satellites.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Skyplot(
                                        satellites = satellites.satellites,
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        bluetoothService.disconnect()
                                        usbService.disconnect()
                                        internalService.disconnect()
                                    },
                                    shape = MaterialTheme.shapes.extraLarge,
                                ) {
                                    Text(
                                        "Disconnect",
                                        style = MaterialTheme.typography.labelLargeEmphasized,
                                    )
                                }
                            }
                        }
                    }
                } else if (connectionType == 0) {
                    BluetoothPicker(bluetoothService)
                } else if (connectionType == 1) {
                    UsbPicker(usbService)
                } else {
                    // Internal GPS — show connect button only when not connected
                    val scope = rememberCoroutineScope()
                    val activity = LocalContext.current as? org.opentopo.app.MainActivity
                    val permissionLauncher = rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        if (granted) {
                            internalService.connect()
                            scope.launch { activity?.prefs?.setConnectionType(2) }
                        }
                    }
                    Text(
                        "Use device\u2019s built-in GPS. No RTK corrections available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            if (internalService.hasPermission()) {
                                internalService.connect()
                                scope.launch { activity?.prefs?.setConnectionType(2) }
                            } else {
                                permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(percent = 50),
                    ) {
                        Icon(Icons.Outlined.PhoneAndroid, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Connect Internal GPS",
                            style = MaterialTheme.typography.labelLargeEmphasized,
                        )
                    }
                }
            }
        }

        // -- NTRIP Corrections --
        SectionLabel("NTRIP Corrections")
        TonalCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {

                val isNtripConnected = ntripState.status == NtripStatus.CONNECTED
                val isNtripConnecting = ntripState.status == NtripStatus.CONNECTING ||
                    ntripState.status == NtripStatus.RECONNECTING

                if (isNtripConnected || isNtripConnecting) {
                    // Connected / connecting state surface
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (isNtripConnecting) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        ContainedLoadingIndicator(
                                            modifier = Modifier.size(24.dp),
                                        )
                                        Text(
                                            when (ntripState.status) {
                                                NtripStatus.RECONNECTING -> "Reconnecting"
                                                else -> "Connecting"
                                            },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                } else {
                                    Text(
                                        "Connected",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { ntripClient.disconnect() },
                                    shape = MaterialTheme.shapes.extraLarge,
                                ) {
                                    Text(
                                        "Stop",
                                        style = MaterialTheme.typography.labelLargeEmphasized,
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            SurveyStatusRow("Mountpoint", ntripState.mountpoint)
                            SurveyStatusRow("Data Rate", ntripState.dataRateFormatted)
                            SurveyStatusRow(
                                "Correction Age",
                                if (ntripState.ageOfCorrectionSeconds >= 0) {
                                    "${ntripState.ageOfCorrectionSeconds}s"
                                } else {
                                    "\u2014"
                                },
                                valueColor = if (ntripState.ageOfCorrectionSeconds >= 0) {
                                    surveyColors.correctionAgeColor(
                                        ntripState.ageOfCorrectionSeconds,
                                    )
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }

                    // Compact streaming-state row
                    NtripStreamingRow(
                        isConnecting = isNtripConnecting,
                        bytesReceived = ntripState.bytesReceived,
                        ageOfCorrectionSeconds = ntripState.ageOfCorrectionSeconds,
                    )

                    // Stale corrections warning chip
                    if (ntripState.ageOfCorrectionSeconds > 5) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    "Corrections stale!",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            icon = {
                                Icon(
                                    Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer,
                                iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = MaterialTheme.colorScheme.error,
                            ),
                        )
                    }

                    ntripState.error?.let {
                        Text(
                            "Error: $it",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    NtripConnectForm(ntripClient)
                }
            }
        }

        Spacer(Modifier.height(0.dp))
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluetoothPicker(bluetoothService: BluetoothGnssService) {
    var expanded by remember { mutableStateOf(false) }
    val devices = remember { bluetoothService.getPairedDevices() }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            TextField(
                value = selectedDevice?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Device") },
                placeholder = { Text("Select device\u2026") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                devices.forEach { device ->
                    DropdownMenuItem(
                        text = { Text(device.name ?: device.address) },
                        onClick = { selectedDevice = device; expanded = false },
                    )
                }
                if (devices.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No paired devices") },
                        onClick = { expanded = false },
                    )
                }
            }
        }
        Button(
            onClick = { selectedDevice?.let { bluetoothService.connect(it) } },
            enabled = selectedDevice != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(percent = 50),
        ) {
            Icon(
                Icons.Outlined.Bluetooth,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Connect Bluetooth",
                style = MaterialTheme.typography.labelLargeEmphasized,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsbPicker(usbService: UsbGnssService) {
    // Observe USB device version from MainActivity to refresh list on hot-plug
    val activity = LocalContext.current as? org.opentopo.app.MainActivity
    val usbVersion by activity?.usbDeviceVersion?.collectAsState() ?: remember { mutableStateOf(0) }
    val baudRate by activity?.prefs?.baudRate?.collectAsState(initial = 115200) ?: remember { mutableStateOf(115200) }
    val drivers = remember(usbVersion) { usbService.getAvailableDevices() }
    var expanded by remember { mutableStateOf(false) }
    var selectedDriver by remember { mutableStateOf<UsbSerialDriver?>(null) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            TextField(
                value = selectedDriver?.device?.productName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Device") },
                placeholder = { Text("Select USB device\u2026") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                drivers.forEach { driver ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                driver.device.productName
                                    ?: "USB ${driver.device.deviceId}",
                            )
                        },
                        onClick = { selectedDriver = driver; expanded = false },
                    )
                }
                if (drivers.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No USB devices found") },
                        onClick = { expanded = false },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { activity?.refreshUsbDevices() },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Refresh", style = MaterialTheme.typography.labelLargeEmphasized)
            }
            Button(
                onClick = {
                    selectedDriver?.let { driver ->
                        usbService.connect(driver, baudRate)
                        scope.launch { activity?.prefs?.setConnectionType(1) }
                    }
                },
                enabled = selectedDriver != null,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(percent = 50),
            ) {
                Icon(
                    Icons.Outlined.Usb,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Connect", style = MaterialTheme.typography.labelLargeEmphasized)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NtripConnectForm(ntripClient: NtripClient) {
    val activity = LocalContext.current as? org.opentopo.app.MainActivity
    val prefs = activity?.prefs
    val scope = rememberCoroutineScope()

    // Load saved values from DataStore
    val savedPreset by prefs?.ntripPresetIndex?.collectAsState(initial = 0) ?: remember { mutableStateOf(0) }
    val savedHost by prefs?.ntripHost?.collectAsState(initial = "") ?: remember { mutableStateOf("") }
    val savedPort by prefs?.ntripPort?.collectAsState(initial = "2101") ?: remember { mutableStateOf("2101") }
    val savedUsername by prefs?.ntripUsername?.collectAsState(initial = "") ?: remember { mutableStateOf("") }
    val savedPassword by prefs?.ntripPassword?.collectAsState(initial = "") ?: remember { mutableStateOf("") }
    val savedMountpoint by prefs?.ntripMountpoint?.collectAsState(initial = "") ?: remember { mutableStateOf("") }

    // Persisted preset index: -1 (legacy "Custom") maps to our Custom button (index 3).
    val initialPreset = if (savedPreset in 0..2) savedPreset else NTRIP_CUSTOM_INDEX
    var selectedPresetIndex by remember { mutableIntStateOf(initialPreset) }
    var host by remember {
        mutableStateOf(
            if (savedHost.isNotBlank()) savedHost else NtripPresets[initialPreset].host,
        )
    }
    var port by remember {
        mutableStateOf(
            if (savedPort.isNotBlank()) savedPort else NtripPresets[initialPreset].port,
        )
    }
    var username by remember { mutableStateOf(savedUsername) }
    var password by remember { mutableStateOf(savedPassword) }
    var mountpoint by remember {
        mutableStateOf(
            if (savedMountpoint.isNotBlank()) savedMountpoint else NtripPresets[initialPreset].mount,
        )
    }
    var mountpoints by remember { mutableStateOf<List<NtripMountpoint>>(emptyList()) }
    var fetchingSourcetable by remember { mutableStateOf(false) }
    val isCustom = selectedPresetIndex == NTRIP_CUSTOM_INDEX

    // Sync when saved values load (DataStore is async)
    LaunchedEffect(savedPreset, savedHost, savedPort, savedUsername, savedPassword, savedMountpoint) {
        if (savedPreset != 0 || savedHost.isNotBlank()) {
            selectedPresetIndex = if (savedPreset in 0..2) savedPreset else NTRIP_CUSTOM_INDEX
            if (savedHost.isNotBlank()) host = savedHost
            if (savedPort.isNotBlank()) port = savedPort
            username = savedUsername
            password = savedPassword
            mountpoint = savedMountpoint
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Caster preset picker — OpenTopo ButtonGroup
        SectionLabel("Caster")
        OpenTopoButtonGroup(
            options = NtripPresets.map { it.label },
            selectedIndex = selectedPresetIndex,
            onSelect = { index ->
                selectedPresetIndex = index
                val preset = NtripPresets[index]
                host = preset.host
                port = preset.port.ifBlank { "2101" }
                mountpoint = preset.mount
                mountpoints = emptyList()
            },
            modifier = Modifier.fillMaxWidth(),
        )

        // Host + Port fields — always visible so user can verify/edit the
        // auto-filled preset values.
        SectionLabel("Server")
        InputRow(
            label = "Host",
            value = host,
            onValueChange = { host = it },
            placeholder = "ntrip.example.com",
        )
        InputRow(
            label = "Port",
            value = port,
            onValueChange = { port = it },
            keyboardType = KeyboardType.Number,
        )

        // Credentials
        SectionLabel("Credentials")
        InputRow(
            label = "User",
            value = username,
            onValueChange = { username = it },
        )
        InputRow(
            label = "Pass",
            value = password,
            onValueChange = { password = it },
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
        )

        // Mountpoint
        SectionLabel("Mountpoint")
        InputRow(
            label = "Mountpoint",
            value = mountpoint,
            onValueChange = { mountpoint = it },
        )

        // Mountpoint picker (when sourcetable fetched)
        if (mountpoints.isNotEmpty()) {
            var mpExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = mpExpanded,
                onExpandedChange = { mpExpanded = it },
            ) {
                TextField(
                    value = mountpoint.ifBlank { "" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select mountpoint") },
                    placeholder = { Text("Select\u2026") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(mpExpanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = mpExpanded,
                    onDismissRequest = { mpExpanded = false },
                ) {
                    mountpoints.forEach { mp ->
                        DropdownMenuItem(
                            text = { Text("${mp.name} (${mp.format})") },
                            onClick = { mountpoint = mp.name; mpExpanded = false },
                        )
                    }
                }
            }
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    fetchingSourcetable = true
                    ntripClient.fetchSourcetable(
                        NtripConfig(
                            "",
                            host,
                            port.toIntOrNull() ?: 2101,
                            "",
                            username,
                            password,
                        ),
                    ) { result ->
                        fetchingSourcetable = false
                        result.onSuccess { mountpoints = it }
                    }
                },
                enabled = !fetchingSourcetable && host.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                if (fetchingSourcetable) {
                    ContainedLoadingIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Fetching", style = MaterialTheme.typography.labelLargeEmphasized)
                } else {
                    Text("Get List", style = MaterialTheme.typography.labelLargeEmphasized)
                }
            }
            Button(
                onClick = {
                    ntripClient.connect(
                        NtripConfig(
                            "",
                            host,
                            port.toIntOrNull() ?: 2101,
                            mountpoint,
                            username,
                            password,
                        ),
                    )
                    // Persist connection details. For legacy storage we keep 0..2 intact
                    // and use -1 for the Custom slot.
                    val persistIndex = if (isCustom) -1 else selectedPresetIndex
                    scope.launch {
                        prefs?.setNtripConfig(
                            persistIndex, host, port, username, password, mountpoint,
                        )
                    }
                },
                enabled = host.isNotBlank() && mountpoint.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(percent = 50),
            ) {
                Text("Connect", style = MaterialTheme.typography.labelLargeEmphasized)
            }
        }
    }
}

/**
 * Compact streaming-state row for the active NTRIP session. Shows a pulsing
 * dot, total bytes received, and age of the last correction packet.
 */
@Composable
private fun NtripStreamingRow(
    isConnecting: Boolean,
    bytesReceived: Long,
    ageOfCorrectionSeconds: Long,
) {
    TonalCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Pulsing dot (reuses the FixStatusPill halo pattern)
            val infiniteTransition = rememberInfiniteTransition(label = "ntripPulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "ntripDotAlpha",
            )
            val dotColor = MaterialTheme.colorScheme.primary
            Canvas(Modifier.size(10.dp)) {
                drawCircle(
                    color = dotColor,
                    radius = size.minDimension * 0.4f,
                    center = Offset(size.width / 2f, size.height / 2f),
                    alpha = alpha,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("NTRIP", style = LabelOverline, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val kb = bytesReceived / 1024.0
                val ageLabel = if (ageOfCorrectionSeconds >= 0) "${ageOfCorrectionSeconds}s" else "\u2014"
                val stateLabel = if (isConnecting) "connecting" else "streaming"
                Text(
                    "%.1f KB \u00b7 $stateLabel \u00b7 age $ageLabel".format(kb),
                    style = MonoDelta,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun SurveyStatusRow(
    label: String,
    value: String,
    detail: String? = null,
    valueColor: Color = Color.Unspecified,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = CoordinateFont,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
            detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
