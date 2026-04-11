package org.opentopo.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.content.Context
import android.graphics.PointF
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
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
private const val TAB_TRANSFORM = 4
private const val TAB_SETTINGS = 5

private data class TabItem(val title: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("GNSS", Icons.Outlined.Cable),
    TabItem("Survey", Icons.Outlined.Straighten),
    TabItem("Stake", Icons.Outlined.NearMe),
    TabItem("Export", Icons.Outlined.FileDownload),
    TabItem("Transform", Icons.Outlined.Calculate),
    TabItem("Config", Icons.Outlined.Settings),
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
    heposTransform: org.opentopo.transform.HeposTransform? = null,
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
    val scope = rememberCoroutineScope()

    remember { MapLibre.getInstance(context) }

    var selectedTab by remember { mutableIntStateOf(TAB_CONNECTION) }
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var hasAnimatedToFirstFix by remember { mutableStateOf(false) }

    // Observe active project's survey points for map display
    val activeProjectId by surveyManager?.activeProjectId?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val activePoints by activeProjectId?.let {
        db.pointDao().getByProject(it).collectAsState(initial = emptyList())
    } ?: remember { mutableStateOf(emptyList()) }

    // Update user location on map when position changes
    LaunchedEffect(position.latitude, position.longitude, position.fixQuality) {
        val map = mapRef ?: return@LaunchedEffect
        if (!position.hasFix) return@LaunchedEffect

        val point = Point.fromLngLat(position.longitude, position.latitude)
        val source = map.style?.getSourceAs<GeoJsonSource>("user-location")
        if (source != null) {
            source.setGeoJson(point)
        }

        // Update dot color based on fix quality
        val fixColor = surveyColors.fixColor(position.fixQuality)
        val colorHex = String.format("#%06X", fixColor.toArgb() and 0xFFFFFF)
        map.style?.getLayerAs<CircleLayer>("user-location-dot")?.setProperties(
            PropertyFactory.circleColor(colorHex),
        )
        map.style?.getLayerAs<CircleLayer>("user-location-glow")?.setProperties(
            PropertyFactory.circleColor(colorHex),
        )

        // Animate to first fix
        if (!hasAnimatedToFirstFix) {
            hasAnimatedToFirstFix = true
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(position.latitude, position.longitude),
                    16.0,
                ),
                1500,
            )
        }
    }

    // Haptic + audio feedback on point recorded
    LaunchedEffect(recordingState.lastRecordedPoint) {
        recordingState.lastRecordedPoint?.let { pt ->
            // Strong haptic confirmation
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(300)
            }

            // Audio chime
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                kotlinx.coroutines.delay(300)
                toneGen.release()
            } catch (_: Exception) {}

            snackbarHostState.showSnackbar(
                "Recorded ${pt.pointId}: E=${"%.3f".format(pt.easting)} N=${"%.3f".format(pt.northing)} \u00B1${"%.3f".format(pt.horizontalAccuracy ?: 0.0)}m"
            )
        }
    }

    // NTRIP disconnect alert — vibration pattern + warning tone
    LaunchedEffect(ntripState.status) {
        if (ntripState.status == NtripStatus.RECONNECTING || ntripState.status == NtripStatus.DISCONNECTED) {
            // Only alert if we were previously connected (not on initial state)
            if (ntripState.lastDataTime > 0) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // Alert pattern: short-short-long
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100, 100, 400), -1))
                }
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                    toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 500)
                    kotlinx.coroutines.delay(600)
                    toneGen.release()
                } catch (_: Exception) {}

                snackbarHostState.showSnackbar("NTRIP connection lost!")
            }
        }
    }

    // Update survey point markers on map when active points change
    LaunchedEffect(activePoints) {
        val map = mapRef ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect

        // Build GeoJSON FeatureCollection from points
        val features = activePoints.mapNotNull { pt ->
            if (pt.latitude == 0.0 && pt.longitude == 0.0) return@mapNotNull null
            val point = Point.fromLngLat(pt.longitude, pt.latitude)
            Feature.fromGeometry(point).apply {
                addStringProperty("id", pt.pointId)
                addNumberProperty("fixQuality", pt.fixQuality)
            }
        }
        val fc = FeatureCollection.fromFeatures(features)

        val source = style.getSourceAs<GeoJsonSource>("survey-points")
        if (source != null) {
            source.setGeoJson(fc)
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

                // ── Scrollable tab row (6 tabs) ──
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                ) {
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
                            TAB_TRANSFORM -> TransformPanel(heposTransform)
                            TAB_SETTINGS -> SettingsPanel()
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        // ── Map fills the screen ──
        var layerMenuExpanded by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        getMapAsync { map ->
                            // Load vathra.xyz vector basemap style from assets
                            val styleJson = ctx.assets.open("style_vathra.json")
                                .bufferedReader().readText()
                            map.setStyle(
                                org.maplibre.android.maps.Style.Builder()
                                    .fromJson(styleJson),
                            ) { style ->
                                map.cameraPosition = CameraPosition.Builder()
                                    .target(LatLng(38.5, 23.8)).zoom(6.0).build()

                                // Add user location source + layers
                                val locationSource = GeoJsonSource(
                                    "user-location",
                                    Point.fromLngLat(23.8, 38.5),
                                )
                                style.addSource(locationSource)

                                style.addLayer(
                                    CircleLayer("user-location-glow", "user-location")
                                        .withProperties(
                                            PropertyFactory.circleRadius(18f),
                                            PropertyFactory.circleColor("#1565C0"),
                                            PropertyFactory.circleOpacity(0.15f),
                                            PropertyFactory.circleStrokeWidth(0f),
                                        )
                                )
                                style.addLayer(
                                    CircleLayer("user-location-dot", "user-location")
                                        .withProperties(
                                            PropertyFactory.circleRadius(8f),
                                            PropertyFactory.circleColor("#1565C0"),
                                            PropertyFactory.circleOpacity(1f),
                                            PropertyFactory.circleStrokeColor("#FFFFFF"),
                                            PropertyFactory.circleStrokeWidth(2.5f),
                                        )
                                )

                                // Survey points source + layers
                                val pointsSource = GeoJsonSource("survey-points")
                                style.addSource(pointsSource)

                                // Point markers - colored circles based on fix quality
                                style.addLayerBelow(
                                    CircleLayer("survey-points-circle", "survey-points")
                                        .withProperties(
                                            PropertyFactory.circleRadius(6f),
                                            PropertyFactory.circleColor(
                                                Expression.match(
                                                    Expression.get("fixQuality"),
                                                    Expression.literal("#C62828"),  // default: no fix red
                                                    Expression.stop(4, "#2E7D32"),  // RTK fix green
                                                    Expression.stop(5, "#EF6C00"),  // RTK float orange
                                                    Expression.stop(2, "#F9A825"),  // DGPS yellow
                                                    Expression.stop(1, "#1565C0"),  // GPS blue
                                                )
                                            ),
                                            PropertyFactory.circleStrokeColor("#FFFFFF"),
                                            PropertyFactory.circleStrokeWidth(1.5f),
                                        ),
                                    "user-location-glow",
                                )

                                // Point labels
                                style.addLayerBelow(
                                    org.maplibre.android.style.layers.SymbolLayer(
                                        "survey-points-labels", "survey-points",
                                    ).withProperties(
                                        PropertyFactory.textField(Expression.get("id")),
                                        PropertyFactory.textFont(arrayOf("Noto Sans Medium")),
                                        PropertyFactory.textSize(11f),
                                        PropertyFactory.textOffset(arrayOf(0f, -1.5f)),
                                        PropertyFactory.textColor("#333333"),
                                        PropertyFactory.textHaloColor("#FFFFFF"),
                                        PropertyFactory.textHaloWidth(1.5f),
                                        PropertyFactory.textAllowOverlap(true),
                                    ),
                                    "user-location-glow",
                                )

                                // Prepare Ktimatologio orthophoto WMS as hidden raster source
                                val ktimaSource = RasterSource(
                                    "ktima-ortho",
                                    TileSet(
                                        "2.2.0",
                                        "http://gis.ktimanet.gr/wms/wmsopen/wmsserver.aspx?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&LAYERS=BASEMAP&SRS=EPSG:900913&BBOX={bbox-epsg-3857}&WIDTH=256&HEIGHT=256&FORMAT=image/jpeg",
                                    ),
                                    256,
                                )
                                style.addSource(ktimaSource)
                                style.addLayerBelow(
                                    RasterLayer("ktima-ortho-layer", "ktima-ortho")
                                        .withProperties(
                                            PropertyFactory.visibility(org.maplibre.android.style.layers.Property.NONE),
                                        ),
                                    "user-location-glow",
                                )
                            }
                            // Tap survey point to show details
                            map.addOnMapClickListener { latLng ->
                                val pixel = map.projection.toScreenLocation(latLng)
                                val features = map.queryRenderedFeatures(
                                    PointF(pixel.x, pixel.y),
                                    "survey-points-circle",
                                )
                                if (features.isNotEmpty()) {
                                    val pointId = features[0].getStringProperty("id")
                                        ?: return@addOnMapClickListener false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Point: $pointId")
                                    }
                                    true
                                } else {
                                    false
                                }
                            }

                            mapRef = map
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // ── Persistent fix status pill (always visible on map) ──
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Colored fix dot
                    Canvas(Modifier.size(12.dp)) {
                        drawCircle(color = surveyColors.fixColor(position.fixQuality))
                    }
                    // Fix label
                    Text(
                        surveyColors.fixLabel(position.fixQuality),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CoordinateFont,
                    )
                }
            }

            // ── Map layer switcher (top-right) ──
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(12.dp),
            ) {
                FilledIconButton(
                    onClick = { layerMenuExpanded = true },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        Icons.Outlined.Layers,
                        contentDescription = "Map layers",
                        modifier = Modifier.size(22.dp),
                    )
                }
                DropdownMenu(
                    expanded = layerMenuExpanded,
                    onDismissRequest = { layerMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Street Map") },
                        onClick = {
                            layerMenuExpanded = false
                            mapRef?.style?.getLayer("ktima-ortho-layer")?.setProperties(
                                PropertyFactory.visibility(org.maplibre.android.style.layers.Property.NONE),
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Orthophoto (Ktimatologio)") },
                        onClick = {
                            layerMenuExpanded = false
                            mapRef?.style?.getLayer("ktima-ortho-layer")?.setProperties(
                                PropertyFactory.visibility(org.maplibre.android.style.layers.Property.VISIBLE),
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Toggle Contours") },
                        onClick = {
                            layerMenuExpanded = false
                            val layer = mapRef?.style?.getLayer("contours-lines")
                            val labelsLayer = mapRef?.style?.getLayer("contours-labels")
                            val currentVis = layer?.visibility?.value
                            val newVis = if (currentVis == org.maplibre.android.style.layers.Property.VISIBLE)
                                org.maplibre.android.style.layers.Property.NONE
                            else
                                org.maplibre.android.style.layers.Property.VISIBLE
                            layer?.setProperties(PropertyFactory.visibility(newVis))
                            labelsLayer?.setProperties(PropertyFactory.visibility(newVis))
                        },
                    )
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
            Spacer(Modifier.width(8.dp))
            if (position.hasFix) {
                SatelliteBreakdown(satellites)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    accuracy.horizontalAccuracyM?.let { AccuracyBadge(it, "H") }
                    accuracy.altitudeErrorM?.let { AccuracyBadge(it, "V") }
                }
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
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.small,
            ) {
            Row(
                Modifier
                    .fillMaxWidth()
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
                style = MaterialTheme.typography.bodySmall,
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
