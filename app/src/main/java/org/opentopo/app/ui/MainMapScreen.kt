package org.opentopo.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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

private const val TAB_CONNECTION = 0
private const val TAB_SURVEY = 1
private const val TAB_STAKEOUT = 2
private const val TAB_EXPORT = 3

private data class TabItem(val title: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Connect", Icons.Outlined.Cable),
    TabItem("Survey", Icons.Outlined.Straighten),
    TabItem("Stakeout", Icons.Outlined.NearMe),
    TabItem("Export", Icons.Outlined.FileDownload),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val projectedCoords by surveyManager?.projectedPosition?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val recordingState =
        surveyManager?.recordingState?.collectAsState()?.value ?: RecordingState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    remember { MapLibre.getInstance(context) }

    var selectedTab by remember { mutableIntStateOf(TAB_CONNECTION) }
    var fabMenuExpanded by remember { mutableStateOf(false) }

    // Snackbar on point recorded
    LaunchedEffect(recordingState.lastRecordedPoint) {
        recordingState.lastRecordedPoint?.let { pt ->
            snackbarHostState.showSnackbar(
                "Recorded ${pt.pointId}: E=${"%.3f".format(pt.easting)} N=${"%.3f".format(pt.northing)} \u00B1${"%.3f".format(pt.horizontalAccuracy ?: 0.0)}m"
            )
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
        ),
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = modifier,
        sheetPeekHeight = 148.dp,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShadowElevation = 8.dp,
        sheetDragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Box(Modifier.size(width = 32.dp, height = 4.dp))
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 160.dp),
            )
        },
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                // ── Status bar (visible in peek) ──
                StatusBar(
                    position, accuracy, satellites,
                    connectionStatus, ntripState, projectedCoords, surveyColors,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ── Expressive SecondaryTabRow ──
                SecondaryTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    tab.title,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                    }
                }

                // ── Panel content with animated transitions ──
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "panel",
                ) { tab ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                    ) {
                        when (tab) {
                            TAB_CONNECTION -> ConnectionPanel(
                                gnssState, bluetoothService, usbService, ntripClient,
                            )
                            TAB_SURVEY -> SurveyPanel(db, surveyManager)
                            TAB_STAKEOUT -> StakeoutPanel(stakeout)
                            TAB_EXPORT -> ExportPanel(db)
                        }
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
                                    .target(LatLng(38.5, 23.8)).zoom(6.0).build()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // ── HorizontalFloatingToolbar: GNSS status overlay ──
            HorizontalFloatingToolbar(
                expanded = true,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(12.dp),
            ) {
                IconButton(onClick = {}) {
                    FixTypeBadge(position.fixQuality)
                }
                if (position.hasFix) {
                    Icon(
                        Icons.Outlined.SatelliteAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${position.numSatellites}",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = CoordinateFont,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    accuracy.horizontalAccuracyM?.let { AccuracyBadge(it, "H") }
                    if (ntripState.status == NtripStatus.CONNECTED) {
                        Spacer(Modifier.width(4.dp))
                        NtripIndicator(
                            surveyColors.correctionAgeColor(ntripState.ageOfCorrectionSeconds),
                        )
                    }
                }
            }

            // ── FloatingActionButtonMenu: Record actions ──
            val showFab = position.hasFix
                    && surveyManager != null
                    && surveyManager.activeProjectId.collectAsState().value != null

            AnimatedVisibility(
                visible = showFab,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 164.dp),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                if (recordingState.isRecording) {
                    // Recording in progress: show a standalone FAB with progress
                    Box(contentAlignment = Alignment.Center) {
                        LoadingIndicator(
                            progress = { recordingState.progress },
                            modifier = Modifier.size(80.dp),
                            color = surveyColors.recordingProgress,
                        )
                        Surface(
                            color = surveyColors.recordingActive,
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.size(64.dp),
                            shadowElevation = 6.dp,
                            onClick = { surveyManager?.cancelRecording() },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "${recordingState.epochsCollected}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = CoordinateFont,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                } else {
                    // Idle: show FAB menu with survey actions
                    FloatingActionButtonMenu(
                        expanded = fabMenuExpanded,
                        button = {
                            ToggleFloatingActionButton(
                                checked = fabMenuExpanded,
                                onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                            ) {
                                val imageVector by remember {
                                    derivedStateOf {
                                        if (checkedProgress > 0.5f) Icons.Filled.Close
                                        else Icons.Filled.Add
                                    }
                                }
                                Icon(
                                    painter = rememberVectorPainter(imageVector),
                                    contentDescription = "Survey actions",
                                )
                            }
                        },
                    ) {
                        FloatingActionButtonMenuItem(
                            onClick = {
                                fabMenuExpanded = false
                                surveyManager?.startRecording()
                            },
                            icon = {
                                Icon(
                                    Icons.Outlined.RadioButtonChecked,
                                    contentDescription = null,
                                )
                            },
                            text = { Text("Record Point") },
                        )
                        FloatingActionButtonMenuItem(
                            onClick = {
                                fabMenuExpanded = false
                                surveyManager?.startRecording()
                            },
                            icon = {
                                Icon(
                                    Icons.Outlined.Speed,
                                    contentDescription = null,
                                )
                            },
                            text = { Text("Quick Mark") },
                        )
                        FloatingActionButtonMenuItem(
                            onClick = {
                                fabMenuExpanded = false
                                selectedTab = TAB_STAKEOUT
                            },
                            icon = {
                                Icon(
                                    Icons.Outlined.NearMe,
                                    contentDescription = null,
                                )
                            },
                            text = { Text("Stakeout") },
                        )
                    }
                }
            }
        }
    }
}

// ── Status bar shown in bottom sheet peek ──

@Composable
private fun StatusBar(
    position: org.opentopo.app.gnss.PositionState,
    accuracy: org.opentopo.app.gnss.AccuracyState,
    satellites: org.opentopo.app.gnss.SatelliteState,
    connectionStatus: ConnectionStatus,
    ntripState: org.opentopo.app.ntrip.NtripState,
    projectedCoords: org.opentopo.transform.ProjectedCoordinate?,
    surveyColors: org.opentopo.app.ui.theme.SurveyColors,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
    ) {
        // Row 1: Fix badge + sats + accuracy
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FixTypeBadge(position.fixQuality)
            Spacer(Modifier.width(10.dp))
            if (position.hasFix) {
                SatelliteBreakdown(satellites)
                Spacer(Modifier.weight(1f))
                accuracy.horizontalAccuracyM?.let { AccuracyBadge(it, "H") }
                Spacer(Modifier.width(6.dp))
                accuracy.altitudeErrorM?.let { AccuracyBadge(it, "V") }
            } else {
                Spacer(Modifier.weight(1f))
                Text(
                    if (connectionStatus == ConnectionStatus.CONNECTED) "Waiting for fix\u2026"
                    else "Swipe up to connect",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Row 2: Coordinates
        if (position.hasFix) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                projectedCoords?.let {
                    Text(
                        "E ${"%.3f".format(it.eastingM)}",
                        fontFamily = CoordinateFont,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "N ${"%.3f".format(it.northingM)}",
                        fontFamily = CoordinateFont,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } ?: run {
                    Text(
                        "%.8f\u00B0".format(position.latitude),
                        fontFamily = CoordinateFont,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "%.8f\u00B0".format(position.longitude),
                        fontFamily = CoordinateFont,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Row 3: Alt + NTRIP status
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                position.altitude?.let {
                    Text(
                        "Alt: ${"%.2f".format(it)}m",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } ?: Spacer(Modifier.width(1.dp))

                if (ntripState.status == NtripStatus.CONNECTED) {
                    val ageColor =
                        surveyColors.correctionAgeColor(ntripState.ageOfCorrectionSeconds)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        NtripIndicator(ageColor)
                        Text(
                            "NTRIP ${"%.0f".format(ntripState.dataRateBps / 1024.0)}KB/s",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = CoordinateFont,
                            color = ageColor,
                        )
                    }
                }
            }
        }
    }
}

// ── Reusable composables ──

@Composable
private fun NtripIndicator(color: Color) {
    Canvas(Modifier.size(8.dp)) { drawCircle(color) }
}

@Composable
fun FixTypeBadge(fixQuality: Int) {
    val surveyColors = LocalSurveyColors.current
    val color = surveyColors.fixColor(fixQuality)
    val label = surveyColors.fixLabel(fixQuality)
    Surface(
        color = color,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            label,
            color = surveyColors.onFix,
            fontFamily = CoordinateFont,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun SatelliteBreakdown(satellites: org.opentopo.app.gnss.SatelliteState) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        satellites.byConstellation.forEach { (constellation, sats) ->
            val label = when (constellation) {
                org.opentopo.app.gnss.Constellation.GPS -> "GP"
                org.opentopo.app.gnss.Constellation.GLONASS -> "GL"
                org.opentopo.app.gnss.Constellation.GALILEO -> "GA"
                org.opentopo.app.gnss.Constellation.BEIDOU -> "BD"
                else -> "?"
            }
            Text(
                "${sats.size}$label",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = CoordinateFont,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AccuracyBadge(accuracyM: Double, prefix: String) {
    val surveyColors = LocalSurveyColors.current
    val color = surveyColors.accuracyColor(accuracyM)
    Text(
        "$prefix\u00B1${"%.3f".format(accuracyM)}m",
        color = color,
        fontFamily = CoordinateFont,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}
