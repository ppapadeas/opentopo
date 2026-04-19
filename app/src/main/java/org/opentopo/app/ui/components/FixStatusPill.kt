package org.opentopo.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.opentopo.app.ui.theme.CoordinateFont
import org.opentopo.app.ui.theme.DgpsPillContainer
import org.opentopo.app.ui.theme.DgpsPillContainerDark
import org.opentopo.app.ui.theme.DgpsPillOn
import org.opentopo.app.ui.theme.DgpsPillOnDark
import org.opentopo.app.ui.theme.FloatPillContainer
import org.opentopo.app.ui.theme.FloatPillContainerDark
import org.opentopo.app.ui.theme.FloatPillOn
import org.opentopo.app.ui.theme.FloatPillOnDark
import org.opentopo.app.ui.theme.GpsPillContainer
import org.opentopo.app.ui.theme.GpsPillContainerDark
import org.opentopo.app.ui.theme.GpsPillOn
import org.opentopo.app.ui.theme.GpsPillOnDark
import org.opentopo.app.ui.theme.NonePillContainer
import org.opentopo.app.ui.theme.NonePillContainerDark
import org.opentopo.app.ui.theme.NonePillOn
import org.opentopo.app.ui.theme.NonePillOnDark
import org.opentopo.app.ui.theme.RtkPillContainer
import org.opentopo.app.ui.theme.RtkPillContainerDark
import org.opentopo.app.ui.theme.RtkPillOn
import org.opentopo.app.ui.theme.RtkPillOnDark

/**
 * Persistent fix-status pill, OpenTopo v2.0.
 *
 * Layout: `[dot ●] [label]  [σ / sats / extras]`
 * Background tint and label color follow the v2 semantic ramp per fix quality.
 * The dot pulses via an outward halo ring (matching the CSS `::after { pulse }` in the
 * design prototype) when the fix is transitional (None / Float / Acquiring).
 *
 * @param fixQuality NMEA GGA fix quality (0 none, 1 GPS, 2 DGPS, 4 RTK Fix, 5 RTK Float).
 * @param extras Optional trailing monospace metadata, e.g. `"0.9 cm · 22 sats"`.
 */
@Composable
fun FixStatusPill(
    fixQuality: Int,
    modifier: Modifier = Modifier,
    extras: String? = null,
) {
    val dark = isSystemInDarkTheme()

    val (container, content, label) = fixPillTokens(fixQuality, dark)

    // Dot pulse — opacity flutter
    val infiniteTransition = rememberInfiniteTransition(label = "fixPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotAlpha",
    )

    // Halo ring — expands out from the dot, only for transitional states
    val showHalo = fixQuality == 0 || fixQuality == 5
    val haloProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "halo",
    )

    Surface(
        color = container,
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Canvas(Modifier.size(16.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val dotRadius = size.minDimension * 0.25f
                if (showHalo) {
                    // Halo ring expands from dot radius to ~1.8× and fades out
                    val ringRadius = dotRadius * (0.7f + haloProgress * 1.1f)
                    val ringAlpha = (1f - haloProgress).coerceIn(0f, 1f) * 0.6f
                    drawCircle(
                        color = content,
                        radius = ringRadius,
                        center = center,
                        alpha = ringAlpha,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
                drawCircle(color = content, radius = dotRadius, center = center, alpha = dotAlpha)
            }
            Text(
                label,
                color = content,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = CoordinateFont,
            )
            if (!extras.isNullOrBlank()) {
                Text(
                    "· $extras",
                    color = content.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = CoordinateFont,
                )
            }
        }
    }
}

private data class PillTokens(val container: Color, val content: Color, val label: String)

private fun fixPillTokens(fixQuality: Int, dark: Boolean): PillTokens = when (fixQuality) {
    4 -> PillTokens(
        container = if (dark) RtkPillContainerDark else RtkPillContainer,
        content = if (dark) RtkPillOnDark else RtkPillOn,
        label = "RTK Fix",
    )
    5 -> PillTokens(
        container = if (dark) FloatPillContainerDark else FloatPillContainer,
        content = if (dark) FloatPillOnDark else FloatPillOn,
        label = "RTK Float",
    )
    2 -> PillTokens(
        container = if (dark) DgpsPillContainerDark else DgpsPillContainer,
        content = if (dark) DgpsPillOnDark else DgpsPillOn,
        label = "DGPS",
    )
    1 -> PillTokens(
        container = if (dark) GpsPillContainerDark else GpsPillContainer,
        content = if (dark) GpsPillOnDark else GpsPillOn,
        label = "GPS",
    )
    else -> PillTokens(
        container = if (dark) NonePillContainerDark else NonePillContainer,
        content = if (dark) NonePillOnDark else NonePillOn,
        label = "No Fix",
    )
}
