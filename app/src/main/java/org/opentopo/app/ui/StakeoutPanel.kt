package org.opentopo.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import org.opentopo.app.gnss.GnssState
import org.opentopo.app.survey.Stakeout
import org.opentopo.app.survey.StakeoutTarget
import org.opentopo.app.ui.components.FixStatusPill
import org.opentopo.app.ui.components.survey.CompassRing
import org.opentopo.app.ui.components.survey.RecordButton
import org.opentopo.app.ui.theme.CoordinateFont
import org.opentopo.app.ui.theme.LabelOverline
import org.opentopo.app.ui.theme.LocalSurveyColors
import org.opentopo.app.ui.theme.MonoCoord
import org.opentopo.app.ui.theme.MonoDelta
import org.opentopo.app.ui.theme.StakeoutClose
import org.opentopo.app.ui.theme.StakeoutCloseDark
import org.opentopo.app.ui.theme.StakeoutFar
import org.opentopo.app.ui.theme.StakeoutFarDark
import org.opentopo.app.ui.theme.StakeoutOnPoint
import org.opentopo.app.ui.theme.StakeoutOnPointDark

/**
 * Remember a device heading (clockwise from true north, degrees) driven by
 * TYPE_ROTATION_VECTOR. Falls back to a null listener (heading = 0f) on devices
 * without a rotation-vector sensor.
 */
@Composable
private fun rememberDeviceHeadingDeg(): Float {
    val context = LocalContext.current
    var heading by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE)
            as? android.hardware.SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)

        val listener = object : android.hardware.SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3)

            override fun onSensorChanged(event: android.hardware.SensorEvent) {
                android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                android.hardware.SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val deg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                heading = ((deg + 360f) % 360f)
            }

            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) = Unit
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, android.hardware.SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            if (rotationSensor != null) {
                sensorManager.unregisterListener(listener)
            }
        }
    }
    return heading
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StakeoutPanel(
    stakeout: Stakeout?,
    onImmersiveRequest: (() -> Unit)? = null,
    onPipRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val result by stakeout?.result?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
    val currentTarget by stakeout?.target?.collectAsState() ?: remember { mutableStateOf(null) }
    var targetName by remember { mutableStateOf("") }
    var targetE by remember { mutableStateOf("") }
    var targetN by remember { mutableStateOf("") }

    // Sync text fields when target is set externally (e.g. from trig point dialog)
    LaunchedEffect(currentTarget) {
        currentTarget?.let { t ->
            targetName = t.name
            targetE = "%.3f".format(t.easting)
            targetN = "%.3f".format(t.northing)
        }
    }

    val hasTarget = currentTarget != null

    if (!hasTarget) {
        // ─── Target-entry form (preserved from v1) ───────────────────────────
        StakeoutTargetForm(
            stakeout = stakeout,
            targetName = targetName,
            targetE = targetE,
            targetN = targetN,
            onTargetName = { targetName = it },
            onTargetE = { targetE = it },
            onTargetN = { targetN = it },
            modifier = modifier,
        )
    } else {
        StakeoutHud(
            stakeout = stakeout,
            result = result,
            targetName = currentTarget?.name ?: targetName,
            targetEasting = currentTarget?.easting,
            targetNorthing = currentTarget?.northing,
            onImmersiveRequest = onImmersiveRequest,
            onPipRequest = onPipRequest,
            onClearTarget = {
                stakeout?.setTarget(null)
                targetName = ""
                targetE = ""
                targetN = ""
            },
            modifier = modifier,
        )
    }
}

/**
 * v2 immersive Stakeout HUD — shown once a target is active.
 *
 * Layout (top → bottom):
 *   1. Status strip — FixStatusPill + close/exit
 *   2. Target card  — name + EGSA87 E/N
 *   3. CompassRing  — 240 dp focal navigation dial
 *   4. ΔE / ΔN / ΔH readouts with tolerance coloring
 *   5. σH and bearing accuracy footer
 *   6. Action row   — RecordButton (on-point gated) + Full Screen / PiP
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StakeoutHud(
    stakeout: Stakeout?,
    result: org.opentopo.app.survey.StakeoutResult?,
    targetName: String,
    targetEasting: Double?,
    targetNorthing: Double?,
    onImmersiveRequest: (() -> Unit)?,
    onPipRequest: (() -> Unit)?,
    onClearTarget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ── Hoisted derivations (previously done inline in the old Canvas) ──
    val bearingToTarget = result?.bearingDeg?.toFloat() ?: 0f
    val horizontalDelta: Double? = result?.distance
    val deltaE: Double? = result?.deltaEasting
    val deltaN: Double? = result?.deltaNorthing
    // ΔH is not available from the current Stakeout result (horizontal only);
    // surface as null so the row shows the "—" placeholder in neutral color.
    val deltaH: Double? = null

    val deviceHeading = rememberDeviceHeadingDeg()
    val onPoint = remember(horizontalDelta) {
        horizontalDelta != null && horizontalDelta < 0.05
    }

    // Live GNSS telemetry from the shared GnssState pipeline (Stakeout.gnssState
    // is public in v2). Falls back to a coarse "no fix" status when the
    // Stakeout instance itself is null.
    val gnssPos by stakeout?.gnssState?.position?.collectAsState()
        ?: remember { mutableStateOf(org.opentopo.app.gnss.PositionState()) }
    val gnssAcc by stakeout?.gnssState?.accuracy?.collectAsState()
        ?: remember { mutableStateOf(org.opentopo.app.gnss.AccuracyState()) }
    val fixQuality = gnssPos.fixQuality
    val sigmaH = gnssAcc.horizontalAccuracyM
    val numSats = gnssPos.numSatellites

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── 1. Top status strip ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FixStatusPill(
                    fixQuality = fixQuality,
                    extras = sigmaH?.let { "${"%.1f".format(it * 100)} cm · $numSats sats" },
                )
                IconButton(onClick = onClearTarget) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear target",
                    )
                }
            }

            // ── 2. Target card ─────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "TARGET · EGSA87",
                    style = LabelOverline,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    targetName.ifBlank { "—" },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (targetEasting != null && targetNorthing != null) {
                    Text(
                        "E ${"%.3f".format(targetEasting)}  N ${"%.3f".format(targetNorthing)}",
                        style = MonoDelta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── 3. CompassRing (focal element) ─────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (result == null) {
                    // Waiting-for-fix shim so the layout doesn't collapse.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 32.dp),
                    ) {
                        ContainedLoadingIndicator(modifier = Modifier.size(48.dp))
                        Text(
                            "Waiting for fix\u2026",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    CompassRing(
                        currentHeadingDeg = deviceHeading,
                        bearingToTargetDeg = bearingToTarget,
                        distanceMeters = horizontalDelta,
                        ringDiameter = 240.dp,
                        onPoint = onPoint,
                    )
                }
            }

            // ── 4. ΔE / ΔN / ΔH readouts ──────────────────────────────────
            if (result != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DeltaRow(label = "\u0394E", value = deltaE)
                    DeltaRow(label = "\u0394N", value = deltaN)
                    DeltaRow(label = "\u0394H", value = deltaH)
                }

                // ── 5. Accuracy footer ────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("\u03C3H", style = LabelOverline, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        sigmaH?.let { "${"%.3f".format(it)} m" } ?: "—",
                        style = MonoDelta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(24.dp))
                    Text("BRG", style = LabelOverline, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${"%.1f".format(result.bearingDeg)}\u00B0",
                        style = MonoDelta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // ── 6. Bottom action row ──────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RecordButton(
                        onClick = { /* Stake record — wired via parent in later work. */ },
                        enabled = onPoint,
                        isRecording = false,
                        progress = 0f,
                        contentDescription = "Record staked point",
                    )
                    if (onImmersiveRequest != null) {
                        FilledTonalButton(
                            onClick = onImmersiveRequest,
                            shape = RoundedCornerShape(percent = 50),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Fullscreen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Full")
                        }
                    }
                    if (onPipRequest != null) {
                        FilledTonalButton(
                            onClick = onPipRequest,
                            shape = RoundedCornerShape(percent = 50),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PictureInPicture,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("PiP")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Delta readout row — overline label + monospace signed value, color-coded by
 * magnitude using the [StakeoutFar]/[StakeoutClose]/[StakeoutOnPoint] ramp.
 */
@Composable
private fun DeltaRow(label: String, value: Double?) {
    val dark = isSystemInDarkTheme()
    val color: Color = when {
        value == null -> MaterialTheme.colorScheme.onSurfaceVariant
        abs(value) < 0.02 -> if (dark) StakeoutOnPointDark else StakeoutOnPoint
        abs(value) < 0.05 -> if (dark) StakeoutCloseDark else StakeoutClose
        else -> if (dark) StakeoutFarDark else StakeoutFar
    }
    val formatted = when {
        value == null -> "—"
        value >= 0 -> "+${"%.3f".format(value)} m"
        else -> "${"%.3f".format(value)} m"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            label,
            style = LabelOverline,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(26.dp),
        )
        Text(
            formatted,
            style = MonoCoord.copy(fontSize = 28.sp),
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Target-entry form shown while no stakeout target has been set.
 *
 * Preserves the v1 CSV import launcher and manual name/E/N entry form so
 * externally-set targets (trig-point dialog etc.) still flow through.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StakeoutTargetForm(
    stakeout: Stakeout?,
    targetName: String,
    targetE: String,
    targetN: String,
    onTargetName: (String) -> Unit,
    onTargetE: (String) -> Unit,
    onTargetN: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val input = context.contentResolver.openInputStream(it) ?: return@let
                val reader = input.bufferedReader()
                reader.readLine() // skip header
                val firstLine = reader.readLine() ?: return@let
                val fields = firstLine.split(",")
                if (fields.size >= 3) {
                    onTargetName(fields[0].trim())
                    onTargetE(fields[1].trim())
                    onTargetN(fields[2].trim())
                }
                input.close()
            } catch (_: Exception) {
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { importLauncher.launch("text/*") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Icon(Icons.Outlined.FileUpload, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Import Target from CSV")
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp,
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Target (EGSA87)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextField(
                    value = targetName,
                    onValueChange = onTargetName,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = targetE,
                        onValueChange = onTargetE,
                        label = { Text("E (m)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )
                    TextField(
                        value = targetN,
                        onValueChange = onTargetN,
                        label = { Text("N (m)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )
                }
                Spacer(Modifier.height(4.dp))
                FilledTonalButton(
                    onClick = {
                        val e = targetE.toDoubleOrNull()
                        val n = targetN.toDoubleOrNull()
                        if (e != null && n != null) {
                            stakeout?.setTarget(
                                StakeoutTarget(targetName.ifBlank { "Target" }, e, n),
                            )
                        }
                    },
                    enabled = targetE.toDoubleOrNull() != null && targetN.toDoubleOrNull() != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(percent = 50),
                ) {
                    Text("Start")
                }
            }
        }
    }
}

// ── Stakeout immersive full-screen overlay (kept from v1, unchanged) ──

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StakeoutImmersiveOverlay(
    stakeout: Stakeout,
    gnssState: GnssState,
    onExit: () -> Unit,
) {
    val surveyColors = LocalSurveyColors.current
    val result by stakeout.result.collectAsState(initial = null)
    val position by gnssState.position.collectAsState()
    val accuracy by gnssState.accuracy.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f),
        color = MaterialTheme.colorScheme.inverseSurface,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            // Top-left: fix status pill
            FixStatusPill(
                position.fixQuality,
                Modifier.align(Alignment.TopStart),
            )

            // Top-right row: accuracy + exit button
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                accuracy.horizontalAccuracyM?.let { h ->
                    Text(
                        "\u00B1${"%.3f".format(h)}m",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = CoordinateFont,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
                IconButton(onClick = onExit) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Exit full screen",
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
            }

            // Centre content
            val r = result
            if (r != null) {
                val distColor = surveyColors.stakeoutColor(r.distance)
                val dark = isSystemInDarkTheme()
                val onPoint = r.distance < 0.05
                val deviceHeading = rememberDeviceHeadingDeg()

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CompassRing(
                        currentHeadingDeg = deviceHeading,
                        bearingToTargetDeg = r.bearingDeg.toFloat(),
                        distanceMeters = r.distance,
                        ringDiameter = 280.dp,
                        onPoint = onPoint,
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "${r.bearingCardinal} (${"%.1f".format(r.bearingDeg)}\u00B0)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }

                // Bottom: delta E / delta N
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "%.3f".format(r.deltaEasting),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = CoordinateFont,
                            color = distColor,
                        )
                        Text(
                            "\u0394E",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.5f),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "%.3f".format(r.deltaNorthing),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = CoordinateFont,
                            color = distColor,
                        )
                        Text(
                            "\u0394N",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            } else {
                // Waiting for fix
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ContainedLoadingIndicator(modifier = Modifier.size(48.dp))
                    Text(
                        "Waiting for fix\u2026",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
            }
        }
    }
}
