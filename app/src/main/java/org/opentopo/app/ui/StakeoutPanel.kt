package org.opentopo.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.PinDrop
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.opentopo.app.gnss.GnssState
import org.opentopo.app.survey.Stakeout
import org.opentopo.app.survey.StakeoutTarget
import org.opentopo.app.survey.TrigPoint
import org.opentopo.app.survey.TrigPointService
import org.opentopo.app.ui.components.FixStatusPill
import org.opentopo.app.ui.theme.CoordinateFont
import org.opentopo.app.ui.theme.LocalSurveyColors

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StakeoutPanel(
    stakeout: Stakeout?,
    trigPointService: TrigPointService? = null,
    gnssState: GnssState? = null,
    onImmersiveRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val surveyColors = LocalSurveyColors.current
    val result by stakeout?.result?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
    var targetName by remember { mutableStateOf("") }
    var targetE by remember { mutableStateOf("") }
    var targetN by remember { mutableStateOf("") }
    var hasTarget by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        if (!hasTarget) {
            // Import target from CSV
            val context = LocalContext.current
            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    try {
                        val input = context.contentResolver.openInputStream(it) ?: return@let
                        val reader = input.bufferedReader()
                        val header = reader.readLine() // skip header
                        val firstLine = reader.readLine() ?: return@let
                        val fields = firstLine.split(",")
                        if (fields.size >= 3) {
                            targetName = fields[0].trim()
                            targetE = fields[1].trim()
                            targetN = fields[2].trim()
                        }
                        input.close()
                    } catch (_: Exception) {}
                }
            }

            OutlinedButton(
                onClick = { importLauncher.launch("text/*") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.FileUpload, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Import Target from CSV")
            }

            // Nearby trig points
            if (trigPointService != null) {
                val scope = rememberCoroutineScope()
                val positionState = gnssState?.position?.collectAsState()
                val currentPos = positionState?.value
                var nearbyPoints by remember { mutableStateOf<List<TrigPoint>>(emptyList()) }
                var loadingNearby by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            loadingNearby = true
                            val lat = currentPos?.latitude ?: 0.0
                            val lon = currentPos?.longitude ?: 0.0
                            if (lat != 0.0 && lon != 0.0) {
                                nearbyPoints = trigPointService.getNearby(lat, lon, 10000)
                            }
                            loadingNearby = false
                        }
                    },
                    enabled = currentPos?.hasFix == true && !loadingNearby,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.PinDrop, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (loadingNearby) "Loading\u2026" else "Nearby Trig Points")
                }

                nearbyPoints.take(5).forEach { tp ->
                    Surface(
                        onClick = {
                            targetName = "GYS ${tp.gysId}"
                            targetE = tp.egsa87E?.let { "%.3f".format(it) } ?: ""
                            targetN = tp.egsa87N?.let { "%.3f".format(it) } ?: ""
                        },
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val statusColor = when (tp.status) {
                                "OK" -> Color(0xFF4CAF50)
                                "DAMAGED" -> Color(0xFFFF9800)
                                "DESTROYED" -> Color(0xFFF44336)
                                "MISSING" -> Color(0xFF9C27B0)
                                else -> Color(0xFF9E9E9E)
                            }
                            Canvas(Modifier.size(10.dp)) { drawCircle(statusColor) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "GYS ${tp.gysId}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = CoordinateFont,
                                )
                                tp.name?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            tp.elevation?.let {
                                Text(
                                    "${it.toInt()}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = CoordinateFont,
                                )
                            }
                        }
                    }
                }
            }

            // Target input form
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
                        onValueChange = { targetName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(
                            value = targetE,
                            onValueChange = { targetE = it },
                            label = { Text("E (m)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                        )
                        TextField(
                            value = targetN,
                            onValueChange = { targetN = it },
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
                                hasTarget = true
                            }
                        },
                        enabled = targetE.toDoubleOrNull() != null && targetN.toDoubleOrNull() != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text("Start")
                    }
                }
            }
        } else {
            val r = result
            if (r == null) {
                // Waiting state with ContainedLoadingIndicator
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ContainedLoadingIndicator(modifier = Modifier.size(48.dp))
                    Text(
                        "Waiting for fix\u2026",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val distColor = surveyColors.stakeoutColor(r.distance)

                // Compass arrow
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StakeoutArrow(bearingDeg = r.bearingDeg, distance = r.distance)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "%.3f m".format(r.distance),
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = CoordinateFont,
                        color = distColor,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${r.bearingCardinal} (%.1f\u00B0)".format(r.bearingDeg),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Delta card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        SurveyStatusRow(
                            "\u0394E",
                            "%.3f m".format(r.deltaEasting),
                        )
                        SurveyStatusRow(
                            "\u0394N",
                            "%.3f m".format(r.deltaNorthing),
                        )
                        SurveyStatusRow("Target", r.target.name)
                    }
                }
            }

            // Full Screen button (when target is active and has fix)
            if (result != null && onImmersiveRequest != null) {
                FilledTonalButton(
                    onClick = onImmersiveRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Fullscreen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Full Screen")
                }
            }

            // Clear Target button (always visible when target is set)
            OutlinedButton(
                onClick = {
                    stakeout?.setTarget(null)
                    hasTarget = false
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Clear Target")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StakeoutArrow(bearingDeg: Double, distance: Double) {
    val surveyColors = LocalSurveyColors.current
    val color = surveyColors.stakeoutColor(distance)
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val textMeasurer = rememberTextMeasurer()
    val cardinalStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = onSurfaceVariant,
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2
        val innerRadius = radius - 16.dp.toPx()

        // Outer ring
        drawCircle(
            color = outlineVariant,
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )

        // Fill circle with stakeout color at low alpha
        drawCircle(
            color = color.copy(alpha = 0.08f),
            radius = radius,
            center = center,
        )

        // Cardinal direction labels
        val cardinals = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
        for ((label, deg) in cardinals) {
            val measured = textMeasurer.measure(label, cardinalStyle)
            val angleRad = Math.toRadians((deg - 90).toDouble())
            val labelRadius = radius - 10.dp.toPx()
            val lx = center.x + (labelRadius * kotlin.math.cos(angleRad)).toFloat() - measured.size.width / 2f
            val ly = center.y + (labelRadius * kotlin.math.sin(angleRad)).toFloat() - measured.size.height / 2f
            drawText(measured, topLeft = Offset(lx, ly))
        }

        // Tick marks at cardinal directions
        val tickLen = 6.dp.toPx()
        for (deg in listOf(0f, 90f, 180f, 270f)) {
            rotate(deg, pivot = center) {
                drawLine(
                    outlineVariant,
                    Offset(center.x, center.y - radius),
                    Offset(center.x, center.y - radius + tickLen),
                    strokeWidth = 1.5f.dp.toPx(),
                )
            }
        }

        // Arrow pointing in bearing direction
        rotate(bearingDeg.toFloat(), pivot = center) {
            val arrowPath = Path().apply {
                moveTo(center.x, center.y - innerRadius * 0.85f)               // tip
                lineTo(center.x - 14.dp.toPx(), center.y - innerRadius * 0.15f) // left
                lineTo(center.x, center.y - innerRadius * 0.30f)               // notch
                lineTo(center.x + 14.dp.toPx(), center.y - innerRadius * 0.15f) // right
                close()
            }
            drawPath(arrowPath, color)
        }
    }
}

// ── Stakeout immersive full-screen overlay ──

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

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Large distance display
                    Text(
                        "%.3f".format(r.distance),
                        style = MaterialTheme.typography.displayLarge,
                        fontFamily = CoordinateFont,
                        color = distColor,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "metres",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.6f),
                    )

                    Spacer(Modifier.height(24.dp))

                    // Larger compass arrow (240dp)
                    ImmersiveStakeoutArrow(
                        bearingDeg = r.bearingDeg,
                        distance = r.distance,
                    )

                    Spacer(Modifier.height(24.dp))

                    // Bearing text
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
                            color = MaterialTheme.colorScheme.inversePrimary,
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
                            color = MaterialTheme.colorScheme.inversePrimary,
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

/**
 * Larger stakeout arrow (240dp) for the immersive overlay.
 * Uses inverse colour scheme to work on dark backgrounds.
 */
@Composable
private fun ImmersiveStakeoutArrow(bearingDeg: Double, distance: Double) {
    val surveyColors = LocalSurveyColors.current
    val color = surveyColors.stakeoutColor(distance)
    val outlineVariant = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.3f)
    val onSurfaceVariant = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)

    val textMeasurer = rememberTextMeasurer()
    val cardinalStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = onSurfaceVariant,
    )

    Canvas(modifier = Modifier.size(240.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2
        val innerRadius = radius - 20.dp.toPx()

        // Outer ring
        drawCircle(
            color = outlineVariant,
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )

        // Fill circle with stakeout color at low alpha
        drawCircle(
            color = color.copy(alpha = 0.10f),
            radius = radius,
            center = center,
        )

        // Cardinal direction labels
        val cardinals = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
        for ((label, deg) in cardinals) {
            val measured = textMeasurer.measure(label, cardinalStyle)
            val angleRad = Math.toRadians((deg - 90).toDouble())
            val labelRadius = radius - 12.dp.toPx()
            val lx = center.x + (labelRadius * kotlin.math.cos(angleRad)).toFloat() - measured.size.width / 2f
            val ly = center.y + (labelRadius * kotlin.math.sin(angleRad)).toFloat() - measured.size.height / 2f
            drawText(measured, topLeft = Offset(lx, ly))
        }

        // Tick marks at cardinal directions
        val tickLen = 8.dp.toPx()
        for (deg in listOf(0f, 90f, 180f, 270f)) {
            rotate(deg, pivot = center) {
                drawLine(
                    outlineVariant,
                    Offset(center.x, center.y - radius),
                    Offset(center.x, center.y - radius + tickLen),
                    strokeWidth = 2f.dp.toPx(),
                )
            }
        }

        // Arrow pointing in bearing direction
        rotate(bearingDeg.toFloat(), pivot = center) {
            val arrowPath = Path().apply {
                moveTo(center.x, center.y - innerRadius * 0.85f)
                lineTo(center.x - 18.dp.toPx(), center.y - innerRadius * 0.15f)
                lineTo(center.x, center.y - innerRadius * 0.30f)
                lineTo(center.x + 18.dp.toPx(), center.y - innerRadius * 0.15f)
                close()
            }
            drawPath(arrowPath, color)
        }
    }
}
