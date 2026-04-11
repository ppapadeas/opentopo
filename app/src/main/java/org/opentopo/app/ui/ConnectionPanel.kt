package org.opentopo.app.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hoho.android.usbserial.driver.UsbSerialDriver
import org.opentopo.app.gnss.BluetoothGnssService
import org.opentopo.app.gnss.ConnectionStatus
import org.opentopo.app.gnss.GnssState
import org.opentopo.app.gnss.UsbGnssService
import org.opentopo.app.ntrip.NtripClient
import org.opentopo.app.ntrip.NtripConfig
import org.opentopo.app.ntrip.NtripMountpoint
import org.opentopo.app.ntrip.NtripStatus
import org.opentopo.app.ui.theme.CoordinateFont
import org.opentopo.app.ui.theme.LocalSurveyColors

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectionPanel(
    gnssState: GnssState,
    bluetoothService: BluetoothGnssService,
    usbService: UsbGnssService,
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
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Section header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        if (connectionType == 0) Icons.Outlined.Bluetooth else Icons.Outlined.Usb,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "GNSS Receiver",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                // BT / USB segmented toggle
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = connectionType == 0,
                        onClick = { connectionType = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = connectionType == 0) {
                                Icon(
                                    Icons.Outlined.Bluetooth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                    ) { Text("Bluetooth") }
                    SegmentedButton(
                        selected = connectionType == 1,
                        onClick = { connectionType = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = connectionType == 1) {
                                Icon(
                                    Icons.Outlined.Usb,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                    ) { Text("USB") }
                }

                val isConnected = connectionStatus == ConnectionStatus.CONNECTED
                val isConnecting = connectionStatus == ConnectionStatus.CONNECTING

                if (isConnected || isConnecting) {
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
                                    val fixColor = surveyColors.fixColor(position.fixQuality)
                                    Text(
                                        position.fixDescription,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = fixColor,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        bluetoothService.disconnect()
                                        usbService.disconnect()
                                    },
                                ) {
                                    Text("Disconnect")
                                }
                            }

                            if (isConnected && position.hasFix) {
                                Spacer(Modifier.height(8.dp))
                                SurveyStatusRow(
                                    "Satellites",
                                    "${position.numSatellites}",
                                    satellites.byConstellation.entries.joinToString(" ") {
                                        "${it.value.size}${it.key.name.take(2)}"
                                    },
                                )
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
                            }
                        }
                    }
                } else if (connectionType == 0) {
                    BluetoothPicker(bluetoothService)
                } else {
                    UsbPicker(usbService)
                }
            }
        }

        // -- NTRIP Corrections --
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Section header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.CellTower,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "NTRIP Corrections",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

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
                                OutlinedButton(onClick = { ntripClient.disconnect() }) {
                                    Text("Stop")
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
                value = selectedDevice?.name ?: "Select device...",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
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
        ) {
            Icon(
                Icons.Outlined.Bluetooth,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Connect Bluetooth")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsbPicker(usbService: UsbGnssService) {
    // Observe USB device version from MainActivity to refresh list on hot-plug
    val activity = LocalContext.current as? org.opentopo.app.MainActivity
    val usbVersion by activity?.usbDeviceVersion?.collectAsState() ?: remember { mutableStateOf(0) }
    val drivers = remember(usbVersion) { usbService.getAvailableDevices() }
    var expanded by remember { mutableStateOf(false) }
    var selectedDriver by remember { mutableStateOf<UsbSerialDriver?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            TextField(
                value = selectedDriver?.device?.productName ?: "Select USB device\u2026",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
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
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Refresh")
            }
            Button(
                onClick = {
                    selectedDriver?.let { driver ->
                        usbService.connect(driver)
                    }
                },
                enabled = selectedDriver != null,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Outlined.Usb,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Connect")
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

    // -1 means "Custom server"
    var selectedPresetIndex by remember { mutableIntStateOf(savedPreset) }
    var host by remember { mutableStateOf(
        if (savedHost.isNotBlank()) savedHost
        else NtripConfig.PRESETS[0].host
    ) }
    var port by remember { mutableStateOf(
        if (savedPort.isNotBlank()) savedPort
        else NtripConfig.PRESETS[0].port.toString()
    ) }
    var username by remember { mutableStateOf(savedUsername) }
    var password by remember { mutableStateOf(savedPassword) }
    var mountpoint by remember { mutableStateOf(savedMountpoint) }
    var mountpoints by remember { mutableStateOf<List<NtripMountpoint>>(emptyList()) }
    var fetchingSourcetable by remember { mutableStateOf(false) }
    val isCustom = selectedPresetIndex == -1

    // Sync when saved values load (DataStore is async)
    LaunchedEffect(savedPreset, savedHost, savedPort, savedUsername, savedPassword, savedMountpoint) {
        if (savedPreset != 0 || savedHost.isNotBlank()) {
            selectedPresetIndex = savedPreset
            if (savedHost.isNotBlank()) host = savedHost
            if (savedPort.isNotBlank()) port = savedPort
            username = savedUsername
            password = savedPassword
            mountpoint = savedMountpoint
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Caster preset picker
        var presetExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = presetExpanded,
            onExpandedChange = { presetExpanded = it },
        ) {
            TextField(
                value = if (isCustom) "Custom server" else NtripConfig.PRESETS.getOrNull(selectedPresetIndex)?.name ?: "Custom server",
                onValueChange = {},
                readOnly = true,
                label = { Text("Caster") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(presetExpanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = presetExpanded,
                onDismissRequest = { presetExpanded = false },
            ) {
                NtripConfig.PRESETS.forEachIndexed { index, preset ->
                    DropdownMenuItem(
                        text = { Text(preset.name) },
                        onClick = {
                            selectedPresetIndex = index
                            host = preset.host
                            port = preset.port.toString()
                            mountpoints = emptyList()
                            mountpoint = ""
                            presetExpanded = false
                        },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Custom server\u2026") },
                    onClick = {
                        selectedPresetIndex = -1
                        host = ""
                        port = "2101"
                        mountpoints = emptyList()
                        mountpoint = ""
                        presetExpanded = false
                    },
                )
            }
        }

        // Custom server: host + port fields
        if (isCustom) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host") },
                    placeholder = { Text("ntrip.example.com") },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
        }

        // Username / Password row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("User") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Pass") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
        }

        // Mountpoint text field
        OutlinedTextField(
            value = mountpoint,
            onValueChange = { mountpoint = it },
            label = { Text("Mountpoint") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Mountpoint picker (when sourcetable fetched)
        if (mountpoints.isNotEmpty()) {
            var mpExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = mpExpanded,
                onExpandedChange = { mpExpanded = it },
            ) {
                TextField(
                    value = mountpoint.ifBlank { "Select..." },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(mpExpanded) },
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
            ) {
                if (fetchingSourcetable) {
                    ContainedLoadingIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Fetching")
                } else {
                    Text("Get List")
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
                    // Persist connection details
                    scope.launch {
                        prefs?.setNtripConfig(
                            selectedPresetIndex, host, port, username, password, mountpoint,
                        )
                    }
                },
                enabled = host.isNotBlank() && mountpoint.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Connect")
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
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
