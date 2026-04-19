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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoho.android.usbserial.driver.UsbSerialDriver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.opentopo.app.gnss.BluetoothGnssService
import org.opentopo.app.gnss.Constellation
import org.opentopo.app.gnss.ConnectionStatus
import org.opentopo.app.gnss.GnssState
import org.opentopo.app.gnss.SatelliteInfo
import org.opentopo.app.gnss.UsbGnssService
import org.opentopo.app.ntrip.NtripClient
import org.opentopo.app.ntrip.NtripConfig
import org.opentopo.app.ntrip.NtripMountpoint
import org.opentopo.app.ntrip.NtripStatus
import org.opentopo.app.ui.components.ButtonGroup as OpenTopoButtonGroup
import org.opentopo.app.ui.components.InputRow
import org.opentopo.app.ui.components.SectionLabel
import org.opentopo.app.ui.theme.ConstellationColors
import org.opentopo.app.ui.theme.CoordinateFont
import org.opentopo.app.ui.theme.FixNoneRed

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

// ── Mono styles matching the mockup ──
private val MonoOverline = TextStyle(
    fontFamily = CoordinateFont,
    fontSize = 10.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.W700,
    letterSpacing = 0.8.sp,
)

private val MonoCellLabel = TextStyle(
    fontFamily = CoordinateFont,
    fontSize = 10.sp,
    lineHeight = 12.sp,
    fontWeight = FontWeight.W500,
    letterSpacing = 0.8.sp,
)

private val MonoCellValue = TextStyle(
    fontFamily = CoordinateFont,
    fontSize = 15.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.W700,
)

private val MonoStatValue = TextStyle(
    fontFamily = CoordinateFont,
    fontSize = 17.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.W700,
)

private val MonoSub = TextStyle(
    fontFamily = CoordinateFont,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.W400,
)

private val MonoStreamLine = TextStyle(
    fontFamily = CoordinateFont,
    fontSize = 12.sp,
    lineHeight = 19.sp,
    fontWeight = FontWeight.W400,
)

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
    val connectionStatus by gnssState.connectionStatus.collectAsState()
    val accuracy by gnssState.accuracy.collectAsState()
    val satellites by gnssState.satellites.collectAsState()
    val ntripState by ntripClient.state.collectAsState()
    val connectedBtDevice by bluetoothService.connectedDevice.collectAsState()
    val activity = LocalContext.current as? org.opentopo.app.MainActivity
    val savedBaud by activity?.prefs?.baudRate?.collectAsState(initial = 115200)
        ?: remember { mutableStateOf(115200) }
    val savedConnType by activity?.prefs?.connectionType?.collectAsState(initial = 0)
        ?: remember { mutableStateOf(0) }

    var connectionType by remember { mutableIntStateOf(savedConnType.coerceIn(0, 2)) }
    LaunchedEffect(savedConnType) { connectionType = savedConnType.coerceIn(0, 2) }

    val isConnected = connectionStatus == ConnectionStatus.CONNECTED
    val isConnecting = connectionStatus == ConnectionStatus.CONNECTING

    // Track elapsed time since connection established — resets on every
    // transition into CONNECTED. Drives the "CONNECTED · mm:ss" overline.
    var connectedAtMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isConnected) {
        if (isConnected) connectedAtMs = System.currentTimeMillis()
    }
    var elapsedSec by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isConnected, connectedAtMs) {
        while (isConnected) {
            elapsedSec = (System.currentTimeMillis() - connectedAtMs) / 1000
            delay(1000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Connection type ButtonGroup ──
        OpenTopoButtonGroup(
            options = listOf("Bluetooth", "USB-OTG", "Internal"),
            selectedIndex = connectionType,
            onSelect = { connectionType = it },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        if (isConnected) {
            val methodLabel = when (connectionType) {
                0 -> "Bluetooth"
                1 -> "USB-OTG"
                2 -> "Internal GPS"
                else -> "Connected"
            }
            val deviceName = when (connectionType) {
                0 -> connectedBtDevice?.name ?: "GNSS Receiver"
                1 -> "USB Serial GNSS"
                2 -> "Internal GPS"
                else -> "GNSS Receiver"
            }
            val deviceMeta = when (connectionType) {
                0 -> buildString {
                    val addr = connectedBtDevice?.address
                    if (!addr.isNullOrBlank()) {
                        append(addr)
                        append(" \u00B7 ")
                    }
                    append("BT SPP")
                }
                1 -> "USB \u00B7 $savedBaud bps"
                else -> methodLabel
            }
            ConnectedReceiverCard(
                deviceName = deviceName,
                deviceMeta = deviceMeta,
                elapsedSec = elapsedSec,
                horizontalAccuracyM = accuracy.horizontalAccuracyM,
                hdop = accuracy.hdop,
                satsTracked = accuracy.activeSatellitePrns.size,
                satsVisible = satellites.satellites.size,
                onDisconnect = {
                    bluetoothService.disconnect()
                    usbService.disconnect()
                    internalService.disconnect()
                },
            )
        } else if (isConnecting) {
            ConnectingCard()
        } else {
            when (connectionType) {
                0 -> BluetoothPicker(bluetoothService)
                1 -> UsbPicker(usbService)
                else -> InternalGpsPicker(internalService)
            }
        }

        // ── Constellations grid (only when we actually have sats) ──
        if (isConnected && satellites.satellites.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            ConstellationsCard(
                satellites = satellites.satellites,
                activePrns = accuracy.activeSatellitePrns,
            )
        }

        // ── Skyplot, surfaced only when connected ──
        if (isConnected && satellites.satellites.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Skyplot(satellites = satellites.satellites)
        }

        // ── NTRIP card ──
        Spacer(Modifier.height(14.dp))
        NtripCard(ntripClient = ntripClient, ntripState = ntripState)

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Connected receiver hero card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ConnectedReceiverCard(
    deviceName: String,
    deviceMeta: String,
    elapsedSec: Long,
    horizontalAccuracyM: Double?,
    hdop: Double?,
    satsTracked: Int,
    satsVisible: Int,
    onDisconnect: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
        Column(Modifier.padding(18.dp)) {
            // Top row: pulsing dot + "CONNECTED · mm:ss"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PulsingDot(color = MaterialTheme.colorScheme.primary, size = 10)
                val elapsedLabel = "%02d:%02d".format(elapsedSec / 60, elapsedSec % 60)
                Text(
                    "CONNECTED \u00B7 $elapsedLabel",
                    style = MonoOverline.copy(letterSpacing = 1.2.sp),
                    color = onContainer,
                )
            }
            Spacer(Modifier.height(6.dp))

            // Device name
            Text(
                deviceName,
                style = TextStyle(
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.W700,
                    letterSpacing = (-0.09).sp,
                ),
                color = onContainer,
            )

            // MAC / baud / protocol
            Text(
                deviceMeta,
                style = MonoSub,
                color = onContainer.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 2.dp),
            )

            Spacer(Modifier.height(14.dp))

            // Bottom row: stats + Disconnect
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatCell(
                    label = "\u03C3H",
                    value = horizontalAccuracyM?.let { formatAccuracy(it) } ?: "\u2014",
                    onColor = onContainer,
                )
                StatCell(
                    label = "HDOP",
                    value = hdop?.let { "%.1f".format(it) } ?: "\u2014",
                    onColor = onContainer,
                )
                StatCell(
                    label = "SATS",
                    value = if (satsVisible > 0) "$satsTracked/$satsVisible" else "\u2014",
                    onColor = onContainer,
                )
                Button(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FixNoneRed,
                        contentColor = Color.White,
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 8.dp,
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 1.dp),
                ) {
                    Text(
                        "Disconnect",
                        style = TextStyle(
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.W700,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, onColor: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            label,
            style = MonoCellLabel,
            color = onColor.copy(alpha = 0.75f),
        )
        Text(
            value,
            style = MonoStatValue,
            color = onColor,
        )
    }
}

/** Horizontal-accuracy formatter — cm if < 1 m, m otherwise. */
private fun formatAccuracy(m: Double): String = if (m < 1.0) {
    "%.1f cm".format(m * 100)
} else {
    "%.2f m".format(m)
}

@Composable
private fun PulsingDot(color: Color, size: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "connDot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "connDotAlpha",
    )
    Canvas(Modifier.size(size.dp)) {
        drawCircle(
            color = color,
            radius = this.size.minDimension / 2f,
            center = Offset(this.size.width / 2f, this.size.height / 2f),
            alpha = alpha,
        )
    }
}

@Composable
private fun ConnectingCard() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ContainedLoadingIndicator(modifier = Modifier.size(24.dp))
            Text(
                "Connecting\u2026",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Constellations grid
// ─────────────────────────────────────────────────────────────────────────────

private data class ConstCell(
    val name: String,
    val constellation: Constellation,
    val color: Color,
)

private val CONSTELLATION_CELLS = listOf(
    ConstCell("GPS", Constellation.GPS, ConstellationColors.gps),
    ConstCell("GLO", Constellation.GLONASS, ConstellationColors.glonass),
    ConstCell("GAL", Constellation.GALILEO, ConstellationColors.galileo),
    ConstCell("BDS", Constellation.BEIDOU, ConstellationColors.beidou),
)

@Composable
private fun ConstellationsCard(
    satellites: List<SatelliteInfo>,
    activePrns: List<Int>,
) {
    val byConst = satellites.groupBy { it.constellation }
    val activePrnSet = activePrns.toSet()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Public,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "CONSTELLATIONS",
                    style = MonoOverline,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))

            // 4-column grid — Row with weighted cells (Compose-native, no LazyGrid overhead)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CONSTELLATION_CELLS.forEach { cell ->
                    val sats = byConst[cell.constellation].orEmpty()
                    val tracked = sats.count { it.prn in activePrnSet }
                    ConstellationCell(
                        name = cell.name,
                        color = cell.color,
                        tracked = tracked,
                        visible = sats.size,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConstellationCell(
    name: String,
    color: Color,
    tracked: Int,
    visible: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Row {
            // 3 dp coloured left border
            Box(
                Modifier
                    .width(3.dp)
                    .height(50.dp)
                    .background(color),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    name,
                    style = MonoCellLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (visible > 0) "$tracked/$visible" else "\u2014",
                    style = MonoCellValue,
                    color = color,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  NTRIP card
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NtripCard(
    ntripClient: NtripClient,
    ntripState: org.opentopo.app.ntrip.NtripState,
) {
    val activity = LocalContext.current as? org.opentopo.app.MainActivity
    val prefs = activity?.prefs
    val scope = rememberCoroutineScope()

    val savedPreset by prefs?.ntripPresetIndex?.collectAsState(initial = 0)
        ?: remember { mutableStateOf(0) }
    val savedHost by prefs?.ntripHost?.collectAsState(initial = "")
        ?: remember { mutableStateOf("") }
    val savedPort by prefs?.ntripPort?.collectAsState(initial = "2101")
        ?: remember { mutableStateOf("2101") }
    val savedUsername by prefs?.ntripUsername?.collectAsState(initial = "")
        ?: remember { mutableStateOf("") }
    val savedPassword by prefs?.ntripPassword?.collectAsState(initial = "")
        ?: remember { mutableStateOf("") }
    val savedMountpoint by prefs?.ntripMountpoint?.collectAsState(initial = "")
        ?: remember { mutableStateOf("") }

    val initialPreset = if (savedPreset in 0..2) savedPreset else NTRIP_CUSTOM_INDEX
    var selectedPresetIndex by remember { mutableIntStateOf(initialPreset) }
    var host by remember {
        mutableStateOf(if (savedHost.isNotBlank()) savedHost else NtripPresets[initialPreset].host)
    }
    var port by remember {
        mutableStateOf(if (savedPort.isNotBlank()) savedPort else NtripPresets[initialPreset].port)
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

    val isNtripConnected = ntripState.status == NtripStatus.CONNECTED
    val isNtripConnecting = ntripState.status == NtripStatus.CONNECTING ||
        ntripState.status == NtripStatus.RECONNECTING

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            // Header row: icon + label + flexible spacer + live/stale chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.CellTower,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "NTRIP CORRECTIONS",
                    style = MonoOverline,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                NtripStatusChip(
                    isConnected = isNtripConnected,
                    isConnecting = isNtripConnecting,
                    isStale = ntripState.ageOfCorrectionSeconds > 5 && isNtripConnected,
                )
            }
            Spacer(Modifier.height(10.dp))

            // Preset ButtonGroup (HEPOS / CivilPOS / SmartNet)
            OpenTopoButtonGroup(
                options = listOf("HEPOS", "CivilPOS", "SmartNet"),
                selectedIndex = selectedPresetIndex.coerceIn(0, 2),
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
            Spacer(Modifier.height(10.dp))

            if (isNtripConnected || isNtripConnecting) {
                // Two-line streaming stats
                Column {
                    Row {
                        Text(
                            "mountpoint \u00B7 ",
                            style = MonoStreamLine,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            ntripState.mountpoint.ifBlank { mountpoint.ifBlank { "\u2014" } },
                            style = MonoStreamLine.copy(fontWeight = FontWeight.W700),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    val age = if (ntripState.ageOfCorrectionSeconds >= 0) {
                        "%.1f s".format(ntripState.ageOfCorrectionSeconds.toDouble())
                    } else {
                        "\u2014"
                    }
                    val rate = ntripState.dataRateFormatted
                    Text(
                        "age \u00B7 $age    $rate    VRS \u00B7 RTCM 3.2",
                        style = MonoStreamLine,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { ntripClient.disconnect() },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Stop corrections",
                        style = MaterialTheme.typography.labelLargeEmphasized,
                    )
                }
                ntripState.error?.let {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                NtripCredentialFields(
                    isCustom = isCustom,
                    host = host,
                    port = port,
                    username = username,
                    password = password,
                    mountpoint = mountpoint,
                    mountpoints = mountpoints,
                    fetchingSourcetable = fetchingSourcetable,
                    onHostChange = { host = it },
                    onPortChange = { port = it },
                    onUsernameChange = { username = it },
                    onPasswordChange = { password = it },
                    onMountpointChange = { mountpoint = it },
                    onFetchList = {
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
                    onConnect = {
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
                        val persistIndex = if (isCustom) -1 else selectedPresetIndex
                        scope.launch {
                            prefs?.setNtripConfig(
                                persistIndex, host, port, username, password, mountpoint,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun NtripStatusChip(isConnected: Boolean, isConnecting: Boolean, isStale: Boolean) {
    val (bg, fg, label) = when {
        isStale -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "stale",
        )
        isConnected -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "live",
        )
        isConnecting -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "sync\u2026",
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "idle",
        )
    }
    Surface(color = bg, shape = RoundedCornerShape(999.dp)) {
        Text(
            label,
            color = fg,
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.W700),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Disconnected pickers (same behaviour as before, restyled to match the
//  surface-container-low cards used elsewhere on the screen).
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluetoothPicker(bluetoothService: BluetoothGnssService) {
    var expanded by remember { mutableStateOf(false) }
    val devices = remember { bluetoothService.getPairedDevices() }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "BLUETOOTH \u00B7 PAIRED RECEIVERS",
                style = MonoOverline,
                color = MaterialTheme.colorScheme.primary,
            )
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
                shape = RoundedCornerShape(999.dp),
            ) {
                Icon(Icons.Outlined.Bluetooth, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Connect Bluetooth", style = MaterialTheme.typography.labelLargeEmphasized)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsbPicker(usbService: UsbGnssService) {
    val activity = LocalContext.current as? org.opentopo.app.MainActivity
    val usbVersion by activity?.usbDeviceVersion?.collectAsState() ?: remember { mutableStateOf(0) }
    val baudRate by activity?.prefs?.baudRate?.collectAsState(initial = 115200)
        ?: remember { mutableStateOf(115200) }
    val drivers = remember(usbVersion) { usbService.getAvailableDevices() }
    var expanded by remember { mutableStateOf(false) }
    var selectedDriver by remember { mutableStateOf<UsbSerialDriver?>(null) }
    val scope = rememberCoroutineScope()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "USB-OTG \u00B7 ATTACHED RECEIVERS",
                style = MonoOverline,
                color = MaterialTheme.colorScheme.primary,
            )
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
                                Text(driver.device.productName ?: "USB ${driver.device.deviceId}")
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
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
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
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Icon(Icons.Outlined.Usb, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Connect", style = MaterialTheme.typography.labelLargeEmphasized)
                }
            }
        }
    }
}

@Composable
private fun InternalGpsPicker(internalService: org.opentopo.app.gnss.InternalGnssService) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as? org.opentopo.app.MainActivity
    val permissionLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            internalService.connect()
            scope.launch { activity?.prefs?.setConnectionType(2) }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "INTERNAL GPS",
                style = MonoOverline,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Use the device\u2019s built-in GPS. No RTK corrections available.",
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
                shape = RoundedCornerShape(999.dp),
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

// ─────────────────────────────────────────────────────────────────────────────
//  NTRIP credential sub-form (used only when disconnected)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NtripCredentialFields(
    isCustom: Boolean,
    host: String,
    port: String,
    username: String,
    password: String,
    mountpoint: String,
    mountpoints: List<NtripMountpoint>,
    fetchingSourcetable: Boolean,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onMountpointChange: (String) -> Unit,
    onFetchList: () -> Unit,
    onConnect: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isCustom) {
            SectionLabel("Server")
            InputRow(label = "Host", value = host, onValueChange = onHostChange, placeholder = "ntrip.example.com")
            InputRow(label = "Port", value = port, onValueChange = onPortChange, keyboardType = KeyboardType.Number)
        }
        SectionLabel("Credentials")
        InputRow(label = "User", value = username, onValueChange = onUsernameChange)
        InputRow(
            label = "Pass",
            value = password,
            onValueChange = onPasswordChange,
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
        )
        SectionLabel("Mountpoint")
        InputRow(label = "Mountpoint", value = mountpoint, onValueChange = onMountpointChange)

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
                ExposedDropdownMenu(expanded = mpExpanded, onDismissRequest = { mpExpanded = false }) {
                    mountpoints.forEach { mp ->
                        DropdownMenuItem(
                            text = { Text("${mp.name} (${mp.format})") },
                            onClick = { onMountpointChange(mp.name); mpExpanded = false },
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onFetchList,
                enabled = !fetchingSourcetable && host.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(999.dp),
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
                onClick = onConnect,
                enabled = host.isNotBlank() && mountpoint.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text("Connect", style = MaterialTheme.typography.labelLargeEmphasized)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Public helper — kept so SurveyPanel / StakeoutPanel can render a
//  label/value row in the same style.
// ─────────────────────────────────────────────────────────────────────────────

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
