package org.opentopo.app.ui.components.survey

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.opentopo.app.ui.theme.AccuracyGood
import org.opentopo.app.ui.theme.AccuracyGoodDark
import org.opentopo.app.ui.theme.AccuracyOk
import org.opentopo.app.ui.theme.AccuracyOkDark
import org.opentopo.app.ui.theme.AccuracyPoor
import org.opentopo.app.ui.theme.AccuracyPoorDark
import org.opentopo.app.ui.theme.LabelOverline
import org.opentopo.app.ui.theme.MonoCoord

/**
 * AccuracyConvergenceRing — OpenTopo v2.0 σH-vs-gate indicator.
 *
 * Compact 96 dp ring for the Survey screen's top-right corner. It tells the
 * surveyor at a glance how close the live horizontal accuracy is to the
 * configured gate:
 *
 * - **Track** — thin neutral ring behind the progress arc.
 * - **Progress arc** — sweeps clockwise from 12 o'clock. Fill ratio is
 *   `targetSigmaH / currentSigmaH`, clamped to 0..1, so the ring fills as σH
 *   drops toward the gate and overshoots (stays full) once it meets it.
 * - **Color band** — green when the fill ≥ 1 (at or better than target),
 *   amber when ≥ 0.5 (within 2×), red otherwise.
 * - **Center readout** — stacked overline "σH" / monospace sigma value /
 *   overline "target 0.020".
 *
 * The numeric transition animates with a 400 ms tween so that sudden σH
 * jumps don't jitter the arc.
 *
 * @param currentSigmaH Live σH estimate in meters, or `null` for "no data".
 * @param targetSigmaH Accuracy gate in meters (e.g. `0.02` for ±2 cm).
 * @param diameter Overall diameter. 96 dp by default.
 */
@Composable
fun AccuracyConvergenceRing(
    currentSigmaH: Double?,
    targetSigmaH: Double,
    modifier: Modifier = Modifier,
    diameter: Dp = 96.dp,
) {
    val dark = isSystemInDarkTheme()

    val rawRatio: Float = if (currentSigmaH == null || currentSigmaH <= 0.0) {
        0f
    } else {
        (targetSigmaH / currentSigmaH).toFloat().coerceIn(0f, 1f)
    }
    val animatedRatio by animateFloatAsState(
        targetValue = rawRatio,
        animationSpec = tween(durationMillis = 400),
        label = "sigmaRatio",
    )

    val arcColor: Color = when {
        currentSigmaH == null -> MaterialTheme.colorScheme.outlineVariant
        animatedRatio >= 1f -> if (dark) AccuracyGoodDark else AccuracyGood
        animatedRatio >= 0.5f -> if (dark) AccuracyOkDark else AccuracyOk
        else -> if (dark) AccuracyPoorDark else AccuracyPoor
    }
    val trackColor: Color = MaterialTheme.colorScheme.outlineVariant

    val sigmaLabel: String = currentSigmaH?.let { "${"%.3f".format(it)} m" } ?: "—"

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(diameter)) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f
            // Background track
            drawCircle(
                color = trackColor,
                radius = size.minDimension / 2f - inset,
                style = Stroke(width = stroke),
            )
            // Progress arc from 12 o'clock, clockwise.
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = animatedRatio * 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "σH",
                style = LabelOverline,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = sigmaLabel,
                style = MonoCoord.copy(fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "target ${"%.3f".format(targetSigmaH)}",
                style = LabelOverline,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
