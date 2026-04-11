package org.opentopo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.opentopo.app.db.AppDatabase
import org.opentopo.app.gnss.BluetoothGnssService
import org.opentopo.app.gnss.ConnectionStatus
import org.opentopo.app.gnss.GnssState
import org.opentopo.app.gnss.UsbGnssService
import org.opentopo.app.ntrip.NtripClient
import org.opentopo.app.ntrip.NtripStatus
import org.opentopo.app.survey.Stakeout
import org.opentopo.app.survey.SurveyManager

// Panel IDs
private const val PANEL_MENU = 0
private const val PANEL_CONNECTION = 1
private const val PANEL_SURVEY = 2
private const val PANEL_STAKEOUT = 3
private const val PANEL_EXPORT = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMapScreen(
    gnssState: GnssState,
    bluetoothService: BluetoothGnssService,
    usbService: UsbGnssService,
    ntripClient: NtripClient,
    db: AppDatabase,
    surveyManager: SurveyManager?,
    stakeout: Stakeout?,
    modifier: Modifier = Modifier,
) {
    val position by gnssState.position.collectAsState()
    val accuracy by gnssState.accuracy.collectAsState()
    val connectionStatus by gnssState.connectionStatus.collectAsState()
    val ntripState by ntripClient.state.collectAsState()
    val context = LocalContext.current

    remember { MapLibre.getInstance(context) }

    var activePanel by remember { mutableIntStateOf(PANEL_MENU) }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = modifier,
        sheetPeekHeight = 120.dp,
        sheetContent = {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                // ── Collapsed: position bar ──
                PositionBar(position, accuracy, connectionStatus, ntripState.status)

                Spacer(Modifier.height(8.dp))

                if (activePanel == PANEL_MENU) {
                    // Tool grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ToolButton(
                            icon = { Icon(Icons.Default.LocationOn, null, Modifier.size(20.dp)) },
                            label = "Connection",
                            onClick = { activePanel = PANEL_CONNECTION },
                            modifier = Modifier.weight(1f),
                        )
                        ToolButton(
                            icon = { Icon(Icons.Default.Build, null, Modifier.size(20.dp)) },
                            label = "Survey",
                            onClick = { activePanel = PANEL_SURVEY },
                            modifier = Modifier.weight(1f),
                        )
                        ToolButton(
                            icon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) },
                            label = "Stakeout",
                            onClick = { activePanel = PANEL_STAKEOUT },
                            modifier = Modifier.weight(1f),
                        )
                        ToolButton(
                            icon = { Icon(Icons.Default.Share, null, Modifier.size(20.dp)) },
                            label = "Export",
                            onClick = { activePanel = PANEL_EXPORT },
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    // Back to menu
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AssistChip(
                            onClick = { activePanel = PANEL_MENU },
                            label = { Text("\u2190 Back") },
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // Active panel content
                    when (activePanel) {
                        PANEL_CONNECTION -> ConnectionPanel(
                            gnssState = gnssState,
                            bluetoothService = bluetoothService,
                            usbService = usbService,
                            ntripClient = ntripClient,
                        )
                        PANEL_SURVEY -> SurveyPanel(
                            db = db,
                            surveyManager = surveyManager,
                        )
                        PANEL_STAKEOUT -> StakeoutPanel(stakeout = stakeout)
                        PANEL_EXPORT -> ExportPanel(db = db)
                    }
                }
            }
        },
    ) { paddingValues ->
        // ── Map fills the screen ──
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        getMapAsync { map ->
                            map.setStyle("https://demotiles.maplibre.org/style.json") {
                                map.cameraPosition = CameraPosition.Builder()
                                    .target(LatLng(38.5, 23.8))
                                    .zoom(6.0)
                                    .build()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Status chips overlay (top-left)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val gnssColor = when (connectionStatus) {
                    ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                    ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.outline
                }
                AssistChip(
                    onClick = { activePanel = PANEL_CONNECTION },
                    label = { Text(
                        when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> position.fixDescription
                            ConnectionStatus.CONNECTING -> "Connecting"
                            else -> "No GNSS"
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )},
                    colors = AssistChipDefaults.assistChipColors(containerColor = gnssColor.copy(alpha = 0.9f)),
                )

                if (position.hasFix) {
                    AssistChip(
                        onClick = {},
                        label = { Text("${position.numSatellites} sats", style = MaterialTheme.typography.labelSmall) },
                    )
                }

                if (ntripState.status == NtripStatus.CONNECTED) {
                    AssistChip(
                        onClick = { activePanel = PANEL_CONNECTION },
                        label = { Text("NTRIP \u2713", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        ),
                    )
                }
            }

            // FAB: Record Point
            if (position.hasFix && surveyManager != null) {
                FloatingActionButton(
                    onClick = {
                        activePanel = PANEL_SURVEY
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 136.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Default.Add, "Record point")
                }
            }
        }
    }
}

@Composable
private fun PositionBar(
    position: org.opentopo.app.gnss.PositionState,
    accuracy: org.opentopo.app.gnss.AccuracyState,
    connectionStatus: ConnectionStatus,
    ntripStatus: NtripStatus,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (position.hasFix) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "%.8f\u00B0, %.8f\u00B0".format(position.latitude, position.longitude),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                )
                accuracy.horizontalAccuracyM?.let {
                    Text(
                        "\u00B1%.3fm".format(it),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(position.fixDescription, style = MaterialTheme.typography.bodySmall)
                position.altitude?.let {
                    Text("Alt: %.1fm".format(it), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Text(
                if (connectionStatus == ConnectionStatus.CONNECTED) "Waiting for fix..."
                else "No GNSS connected",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ToolButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilledIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier.size(48.dp),
        ) {
            icon()
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
