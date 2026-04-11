package org.opentopo.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import org.opentopo.app.survey.RecordingState
import org.opentopo.app.survey.Stakeout
import org.opentopo.app.survey.SurveyManager
import org.opentopo.app.ui.theme.CoordinateFont
import org.opentopo.app.ui.theme.LocalSurveyColors
import org.opentopo.app.ui.theme.OnFixColor
import org.opentopo.app.ui.theme.RecordingActive
import org.opentopo.app.ui.theme.RecordingProgress

private const val PANEL_NONE = -1
private const val PANEL_CONNECTION = 0
private const val PANEL_SURVEY = 1
private const val PANEL_STAKEOUT = 2
private const val PANEL_EXPORT = 3

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
    val surveyColors = LocalSurveyColors.current
    val position by gnssState.position.collectAsState()
    val accuracy by gnssState.accuracy.collectAsState()
    val satellites by gnssState.satellites.collectAsState()
    val connectionStatus by gnssState.connectionStatus.collectAsState()
    val ntripState by ntripClient.state.collectAsState()
    val projectedCoords by surveyManager?.projectedPosition?.collectAsState() ?: remember { mutableStateOf(null) }
    val recordingState = surveyManager?.recordingState?.collectAsState()?.value ?: RecordingState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    remember { MapLibre.getInstance(context) }

    var activePanel by remember { mutableIntStateOf(PANEL_NONE) }
    var drawerExpanded by remember { mutableStateOf(false) }

    // Snackbar on point recorded
    LaunchedEffect(recordingState.lastRecordedPoint) {
        recordingState.lastRecordedPoint?.let { pt ->
            snackbarHostState.showSnackbar(
                "${pt.pointId}: E=${"%.3f".format(pt.easting)} N=${"%.3f".format(pt.northing)} \u00B1${"%.3f".format(pt.horizontalAccuracy ?: 0.0)}m"
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ── Map (fills entire screen, behind everything) ──
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    getMapAsync { map ->
                        map.setStyle("https://demotiles.maplibre.org/style.json") {
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(38.5, 23.8)).zoom(6.0).build()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── Top drawer overlay ──
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopStart)) {
            // Status bar (always visible)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Row 1: Fix badge + satellites + expand toggle
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FixTypeBadge(position.fixQuality)
                        Spacer(Modifier.width(8.dp))
                        if (position.hasFix) {
                            SatelliteBreakdown(satellites)
                            Spacer(Modifier.weight(1f))
                            accuracy.horizontalAccuracyM?.let { AccuracyBadge(it, "H") }
                            Spacer(Modifier.width(4.dp))
                            // NTRIP indicator
                            if (ntripState.status == NtripStatus.CONNECTED) {
                                val ageColor = surveyColors.correctionAgeColor(ntripState.ageOfCorrectionSeconds)
                                Canvas(Modifier.size(8.dp)) { drawCircle(ageColor) }
                                Spacer(Modifier.width(2.dp))
                                Text("${ntripState.ageOfCorrectionSeconds}s", style = MaterialTheme.typography.labelSmall, fontFamily = CoordinateFont, color = ageColor)
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                            if (connectionStatus == ConnectionStatus.DISCONNECTED) {
                                Text("Tap Connection to start", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        IconButton(onClick = { drawerExpanded = !drawerExpanded; if (!drawerExpanded) activePanel = PANEL_NONE }) {
                            Icon(if (drawerExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, "Toggle")
                        }
                    }

                    // Row 2-3: Coordinates (if fix)
                    if (position.hasFix) {
                        Spacer(Modifier.height(2.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            projectedCoords?.let {
                                Text("E ${"%.3f".format(it.eastingM)}", fontFamily = CoordinateFont, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("N ${"%.3f".format(it.northingM)}", fontFamily = CoordinateFont, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            } ?: run {
                                Text("%.8f\u00B0".format(position.latitude), fontFamily = CoordinateFont, style = MaterialTheme.typography.bodyMedium)
                                Text("%.8f\u00B0".format(position.longitude), fontFamily = CoordinateFont, style = MaterialTheme.typography.bodyMedium)
                            }
                            accuracy.altitudeErrorM?.let { AccuracyBadge(it, "V") }
                        }
                    }
                }
            }

            // Expandable area
            AnimatedVisibility(visible = drawerExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        // Tool buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            ToolButton(
                                icon = { Icon(Icons.Default.LocationOn, null, Modifier.size(20.dp)) },
                                label = "Connection",
                                selected = activePanel == PANEL_CONNECTION,
                                onClick = { activePanel = if (activePanel == PANEL_CONNECTION) PANEL_NONE else PANEL_CONNECTION },
                                statusColor = when (connectionStatus) {
                                    ConnectionStatus.CONNECTED -> surveyColors.fixColor(position.fixQuality)
                                    ConnectionStatus.CONNECTING -> surveyColors.rtkFloat
                                    else -> null
                                },
                            )
                            ToolButton(
                                icon = { Icon(Icons.Default.Build, null, Modifier.size(20.dp)) },
                                label = "Survey",
                                selected = activePanel == PANEL_SURVEY,
                                onClick = { activePanel = if (activePanel == PANEL_SURVEY) PANEL_NONE else PANEL_SURVEY },
                            )
                            ToolButton(
                                icon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) },
                                label = "Stakeout",
                                selected = activePanel == PANEL_STAKEOUT,
                                onClick = { activePanel = if (activePanel == PANEL_STAKEOUT) PANEL_NONE else PANEL_STAKEOUT },
                            )
                            ToolButton(
                                icon = { Icon(Icons.Default.Share, null, Modifier.size(20.dp)) },
                                label = "Export",
                                selected = activePanel == PANEL_EXPORT,
                                onClick = { activePanel = if (activePanel == PANEL_EXPORT) PANEL_NONE else PANEL_EXPORT },
                            )
                        }

                        // Active panel content
                        when (activePanel) {
                            PANEL_CONNECTION -> ConnectionPanel(gnssState, bluetoothService, usbService, ntripClient)
                            PANEL_SURVEY -> SurveyPanel(db, surveyManager)
                            PANEL_STAKEOUT -> StakeoutPanel(stakeout)
                            PANEL_EXPORT -> ExportPanel(db)
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }

        // ── FAB: Record Point ──
        if (position.hasFix && surveyManager != null && surveyManager.activeProjectId.collectAsState().value != null) {
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (recordingState.isRecording) {
                    CircularProgressIndicator(
                        progress = { recordingState.progress },
                        modifier = Modifier.size(72.dp),
                        strokeWidth = 4.dp,
                        color = RecordingProgress,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
                FloatingActionButton(
                    onClick = {
                        if (!recordingState.isRecording) {
                            surveyManager.startRecording()
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    containerColor = if (recordingState.isRecording) RecordingActive else MaterialTheme.colorScheme.primary,
                ) {
                    if (recordingState.isRecording) {
                        Text("${recordingState.epochsCollected}", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Add, "Record point", Modifier.size(28.dp))
                    }
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        )
    }
}

// ── Reusable composables ──

@Composable
fun FixTypeBadge(fixQuality: Int) {
    val surveyColors = LocalSurveyColors.current
    val color = surveyColors.fixColor(fixQuality)
    val label = when (fixQuality) { 4 -> "RTK Fix"; 5 -> "RTK Float"; 2 -> "DGPS"; 1 -> "GPS"; else -> "No Fix" }
    Surface(color = color, shape = RoundedCornerShape(4.dp)) {
        Text(label, color = OnFixColor, fontFamily = CoordinateFont, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun SatelliteBreakdown(satellites: org.opentopo.app.gnss.SatelliteState) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        satellites.byConstellation.forEach { (constellation, sats) ->
            val label = when (constellation) {
                org.opentopo.app.gnss.Constellation.GPS -> "GP"
                org.opentopo.app.gnss.Constellation.GLONASS -> "GL"
                org.opentopo.app.gnss.Constellation.GALILEO -> "GA"
                org.opentopo.app.gnss.Constellation.BEIDOU -> "BD"
                else -> "?"
            }
            Text("${sats.size}$label", style = MaterialTheme.typography.labelSmall, fontFamily = CoordinateFont)
        }
    }
}

@Composable
fun AccuracyBadge(accuracyM: Double, prefix: String) {
    val surveyColors = LocalSurveyColors.current
    val color = surveyColors.accuracyColor(accuracyM)
    Text("$prefix\u00B1${"%.3f".format(accuracyM)}m", color = color, fontFamily = CoordinateFont,
        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun ToolButton(
    icon: @Composable () -> Unit,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    statusColor: Color? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            if (statusColor != null) {
                Canvas(Modifier.size(10.dp)) { drawCircle(statusColor) }
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
