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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
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

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionPanel(
    gnssState: GnssState,
    bluetoothService: BluetoothGnssService,
    usbService: UsbGnssService,
    ntripClient: NtripClient,
    modifier: Modifier = Modifier,
) {
    val connectionStatus by gnssState.connectionStatus.collectAsState()
    val position by gnssState.position.collectAsState()
    val ntripState by ntripClient.state.collectAsState()

    // 0 = Bluetooth, 1 = USB
    var connectionType by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Text("Connection", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        // ── GNSS Receiver ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("GNSS Receiver", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                // Transport toggle
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = connectionType == 0,
                        onClick = { connectionType = 0 },
                        label = { Text("Bluetooth") },
                    )
                    FilterChip(
                        selected = connectionType == 1,
                        onClick = { connectionType = 1 },
                        label = { Text("USB") },
                    )
                }

                Spacer(Modifier.height(8.dp))

                val isConnected = connectionStatus == ConnectionStatus.CONNECTED
                val isConnecting = connectionStatus == ConnectionStatus.CONNECTING

                if (isConnected || isConnecting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (isConnecting) "Connecting..." else "Connected",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        OutlinedButton(onClick = {
                            bluetoothService.disconnect()
                            usbService.disconnect()
                        }) { Text("Disconnect") }
                    }
                } else if (connectionType == 0) {
                    // Bluetooth picker
                    BluetoothPicker(bluetoothService)
                } else {
                    // USB picker
                    UsbPicker(usbService)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Position summary (compact) ──
        if (position.hasFix) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(12.dp)) {
                    StatusRow("Fix", position.fixDescription)
                    StatusRow("Lat", "%.8f\u00B0".format(position.latitude))
                    StatusRow("Lon", "%.8f\u00B0".format(position.longitude))
                    StatusRow("Sats", "${position.numSatellites}")
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── NTRIP ──
        NtripSection(ntripClient, ntripState)

        Spacer(Modifier.height(16.dp))
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluetoothPicker(bluetoothService: BluetoothGnssService) {
    var expanded by remember { mutableStateOf(false) }
    val devices = remember { bluetoothService.getPairedDevices() }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selectedDevice?.name ?: "Select device...",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            devices.forEach { device ->
                DropdownMenuItem(
                    text = { Text(device.name ?: device.address) },
                    onClick = { selectedDevice = device; expanded = false },
                )
            }
            if (devices.isEmpty()) {
                DropdownMenuItem(text = { Text("No paired devices") }, onClick = { expanded = false })
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { selectedDevice?.let { bluetoothService.connect(it) } },
        enabled = selectedDevice != null,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Connect Bluetooth") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsbPicker(usbService: UsbGnssService) {
    val drivers = remember { usbService.getAvailableDevices() }
    var expanded by remember { mutableStateOf(false) }
    var selectedDriver by remember { mutableStateOf<UsbSerialDriver?>(null) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selectedDriver?.device?.productName ?: "Select USB device...",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            drivers.forEach { driver ->
                DropdownMenuItem(
                    text = { Text(driver.device.productName ?: "USB Serial ${driver.device.deviceId}") },
                    onClick = { selectedDriver = driver; expanded = false },
                )
            }
            if (drivers.isEmpty()) {
                DropdownMenuItem(text = { Text("No USB devices") }, onClick = { expanded = false })
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { selectedDriver?.let { usbService.connect(it) } },
        enabled = selectedDriver != null,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Connect USB") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NtripSection(ntripClient: NtripClient, ntripState: NtripState) {
    val isConnected = ntripState.status == NtripStatus.CONNECTED
    val isConnecting = ntripState.status == NtripStatus.CONNECTING ||
        ntripState.status == NtripStatus.RECONNECTING

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("NTRIP Corrections", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (isConnected || isConnecting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        when (ntripState.status) {
                            NtripStatus.CONNECTING -> "Connecting..."
                            NtripStatus.RECONNECTING -> "Reconnecting..."
                            else -> "Connected"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedButton(onClick = { ntripClient.disconnect() }) { Text("Stop") }
                }
                Spacer(Modifier.height(4.dp))
                StatusRow("Mountpoint", ntripState.mountpoint)
                StatusRow("Data Rate", ntripState.dataRateFormatted)
                StatusRow("Age", if (ntripState.ageOfCorrectionSeconds >= 0) "${ntripState.ageOfCorrectionSeconds}s" else "\u2014")
                ntripState.error?.let {
                    Text("Error: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                NtripConnectForm(ntripClient)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NtripConnectForm(ntripClient: NtripClient) {
    var selectedPresetIndex by remember { mutableIntStateOf(0) }
    var host by remember { mutableStateOf(NtripConfig.PRESETS[0].host) }
    var port by remember { mutableStateOf(NtripConfig.PRESETS[0].port.toString()) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mountpoint by remember { mutableStateOf("") }
    var mountpoints by remember { mutableStateOf<List<NtripMountpoint>>(emptyList()) }
    var fetchingSourcetable by remember { mutableStateOf(false) }

    // Preset
    var presetExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = presetExpanded, onExpandedChange = { presetExpanded = it }) {
        TextField(
            value = NtripConfig.PRESETS.getOrNull(selectedPresetIndex)?.name ?: "Custom",
            onValueChange = {},
            readOnly = true,
            label = { Text("Caster") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(presetExpanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = presetExpanded, onDismissRequest = { presetExpanded = false }) {
            NtripConfig.PRESETS.forEachIndexed { index, preset ->
                DropdownMenuItem(
                    text = { Text(preset.name) },
                    onClick = {
                        selectedPresetIndex = index; host = preset.host
                        port = preset.port.toString(); mountpoints = emptyList()
                        mountpoint = ""; presetExpanded = false
                    },
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("User") }, modifier = Modifier.weight(1f), singleLine = true)
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Pass") }, modifier = Modifier.weight(1f), singleLine = true, visualTransformation = PasswordVisualTransformation())
    }

    Spacer(Modifier.height(4.dp))
    OutlinedTextField(value = mountpoint, onValueChange = { mountpoint = it }, label = { Text("Mountpoint") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

    // Mountpoint fetch
    if (mountpoints.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        var mpExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = mpExpanded, onExpandedChange = { mpExpanded = it }) {
            TextField(
                value = mountpoint.ifBlank { "Select..." },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(mpExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = mpExpanded, onDismissRequest = { mpExpanded = false }) {
                mountpoints.forEach { mp ->
                    DropdownMenuItem(
                        text = { Text("${mp.name} (${mp.format})") },
                        onClick = { mountpoint = mp.name; mpExpanded = false },
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                fetchingSourcetable = true
                ntripClient.fetchSourcetable(NtripConfig("", host, port.toIntOrNull() ?: 2101, "", username, password)) { result ->
                    fetchingSourcetable = false
                    result.onSuccess { mountpoints = it }
                }
            },
            enabled = !fetchingSourcetable && host.isNotBlank(),
            modifier = Modifier.weight(1f),
        ) { Text(if (fetchingSourcetable) "Fetching..." else "Get List") }

        Button(
            onClick = {
                ntripClient.connect(NtripConfig("", host, port.toIntOrNull() ?: 2101, mountpoint, username, password))
            },
            enabled = host.isNotBlank() && mountpoint.isNotBlank(),
            modifier = Modifier.weight(1f),
        ) { Text("Connect") }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

// Re-export for use elsewhere
private typealias NtripState = org.opentopo.app.ntrip.NtripState
