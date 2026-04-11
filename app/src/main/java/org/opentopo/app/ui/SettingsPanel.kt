package org.opentopo.app.ui

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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.opentopo.app.ui.theme.CoordinateFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(modifier: Modifier = Modifier) {
    val activity = LocalContext.current as? org.opentopo.app.MainActivity
    val prefs = activity?.prefs
    val scope = rememberCoroutineScope()

    // Collect all settings
    val averagingSeconds by prefs?.averagingSeconds?.collectAsState(initial = 5)
        ?: remember { mutableStateOf(5) }
    val minAccuracy by prefs?.minAccuracyM?.collectAsState(initial = "0.05")
        ?: remember { mutableStateOf("0.05") }
    val requireRtk by prefs?.requireRtkFix?.collectAsState(initial = false)
        ?: remember { mutableStateOf(false) }
    val baudRate by prefs?.baudRate?.collectAsState(initial = 115200)
        ?: remember { mutableStateOf(115200) }
    val ggaInterval by prefs?.ggaIntervalSeconds?.collectAsState(initial = 10)
        ?: remember { mutableStateOf(10) }
    val coordFormat by prefs?.coordFormat?.collectAsState(initial = 0)
        ?: remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(
            icon = Icons.Outlined.Settings,
            title = "Settings",
            modifier = Modifier.padding(top = 8.dp),
        )

        // ── Recording settings ──
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Recording", style = MaterialTheme.typography.titleMedium)

                // Averaging seconds
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Averaging time", style = MaterialTheme.typography.bodyMedium)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value = "${averagingSeconds}s",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .width(120.dp)
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                            },
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            listOf(1, 3, 5, 10, 15, 30, 60).forEach { secs ->
                                DropdownMenuItem(
                                    text = { Text("${secs}s") },
                                    onClick = {
                                        scope.launch { prefs?.setAveragingSeconds(secs) }
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                // Min accuracy threshold
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Min accuracy (m)", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = minAccuracy,
                        onValueChange = { scope.launch { prefs?.setMinAccuracyM(it) } },
                        modifier = Modifier.width(120.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = CoordinateFont,
                        ),
                        singleLine = true,
                    )
                }

                // Require RTK Fix toggle
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Require RTK Fix", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = requireRtk,
                        onCheckedChange = {
                            scope.launch { prefs?.setRequireRtkFix(it) }
                        },
                    )
                }
            }
        }

        // ── Connection settings ──
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Connection", style = MaterialTheme.typography.titleMedium)

                // Baud rate
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Baud rate", style = MaterialTheme.typography.bodyMedium)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value = "$baudRate",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .width(120.dp)
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = CoordinateFont,
                            ),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                            },
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            listOf(4800, 9600, 19200, 38400, 57600, 115200, 230400, 460800)
                                .forEach { rate ->
                                    DropdownMenuItem(
                                        text = { Text("$rate") },
                                        onClick = {
                                            scope.launch { prefs?.setBaudRate(rate) }
                                            expanded = false
                                        },
                                    )
                                }
                        }
                    }
                }

                // GGA interval
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("GGA interval", style = MaterialTheme.typography.bodyMedium)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value = "${ggaInterval}s",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .width(120.dp)
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                            },
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            listOf(5, 10, 15, 30, 60).forEach { secs ->
                                DropdownMenuItem(
                                    text = { Text("${secs}s") },
                                    onClick = {
                                        scope.launch { prefs?.setGgaIntervalSeconds(secs) }
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Display settings ──
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Display", style = MaterialTheme.typography.titleMedium)

                // Coordinate format
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Coordinates", style = MaterialTheme.typography.bodyMedium)
                    var expanded by remember { mutableStateOf(false) }
                    val formatLabels = listOf("EGSA87 (E/N)", "WGS84 Decimal", "WGS84 DMS")
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value = formatLabels.getOrElse(coordFormat) { formatLabels[0] },
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .width(180.dp)
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                            },
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            formatLabels.forEachIndexed { index, label ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        scope.launch { prefs?.setCoordFormat(index) }
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── About ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Text("OpenTopo v0.2.0", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Open-source GNSS surveying for Android",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Map data: OpenStreetMap contributors",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    "Orthophoto: Hellenic Cadastre (Ktimatologio)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    "Contours: vathra.xyz",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
