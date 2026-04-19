@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package org.opentopo.app.ui.components.survey

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import org.opentopo.app.ui.theme.LabelOverline
import org.opentopo.app.ui.theme.MonoCoord
import org.opentopo.app.ui.theme.StakeoutOnPoint
import org.opentopo.app.ui.theme.StakeoutOnPointDark

/**
 * CompassRing — OpenTopo v2.0 stakeout navigation dial.
 *
 * 240 dp Canvas-based bearing ring shown on the Stakeout immersive screen.
 * Combines three signals so the surveyor can walk to target without looking
 * away from their feet:
 *
 * - **Outer ring** — a 4 dp ring, neutral by default, glowing green with the
 *   `StakeoutOnPoint` color when the surveyor is within tolerance.
 * - **Cardinal rose** — N / E / S / W labels plus 30° minor ticks, rotated by
 *   `-currentHeadingDeg` so North always tracks true north as the phone turns.
 * - **Bearing arrow** — a filled triangle pointing from the ring edge toward
 *   the target at `(bearingToTarget - currentHeading)`.
 * - **Center readout** — the formatted distance to target (or `—` when
 *   unknown), with a small overline caption underneath.
 *
 * @param currentHeadingDeg Device heading in degrees clockwise from true
 *  north. Animated with a short 150 ms tween to damp magnetometer jitter.
 * @param bearingToTargetDeg Geodesic bearing (0..360) from the current fix to
 *  the stakeout target.
 * @param distanceMeters Horizontal distance to target in meters, or `null`
 *  when no fix is available.
 * @param ringDiameter Overall diameter. Defaults to 240 dp; shrink for
 *  split-screen layouts.
 * @param onPoint When true the whole ring and arrow turn green — the surveyor
 *  is inside the on-point radius.
 */
@Composable
fun CompassRing(
    currentHeadingDeg: Float,
    bearingToTargetDeg: Float,
    distanceMeters: Double?,
    modifier: Modifier = Modifier,
    ringDiameter: Dp = 240.dp,
    onPoint: Boolean = false,
) {
    val dark = isSystemInDarkTheme()
    val onPointColor: Color = if (dark) StakeoutOnPointDark else StakeoutOnPoint
    val ringColor: Color =
        if (onPoint) onPointColor else MaterialTheme.colorScheme.outlineVariant
    val arrowColor: Color =
        if (onPoint) onPointColor else MaterialTheme.colorScheme.primary
    val cardinalPrimary: Color = MaterialTheme.colorScheme.onSurface
    val cardinalSecondary: Color = MaterialTheme.colorScheme.onSurfaceVariant
    val tickColor: Color = MaterialTheme.colorScheme.outlineVariant

    val animatedHeading by animateFloatAsState(
        targetValue = currentHeadingDeg,
        animationSpec = tween(durationMillis = 150),
        label = "compassHeading",
    )
    val animatedBearing by animateFloatAsState(
        targetValue = bearingToTargetDeg,
        animationSpec = tween(durationMillis = 150),
        label = "compassBearing",
    )

    val textMeasurer = rememberTextMeasurer()
    val cardinalBoldStyle = MaterialTheme.typography.labelLargeEmphasized.copy(
        color = cardinalPrimary,
        fontWeight = FontWeight.W800,
    )
    val cardinalStyle = MaterialTheme.typography.labelLargeEmphasized.copy(
        color = cardinalSecondary,
    )

    Box(
        modifier = modifier.size(ringDiameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(ringDiameter)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeRing = 4.dp.toPx()
            val tickStroke = 1.dp.toPx()
            val minorTickLen = 8.dp.toPx()

            // Outer ring
            drawCircle(
                color = ringColor,
                radius = radius - strokeRing / 2f,
                center = center,
                style = Stroke(width = strokeRing),
            )

            // Rotate whole cardinal+tick layer by -heading so N locks to true north.
            rotate(degrees = -animatedHeading, pivot = center) {
                // Minor ticks every 30°.
                for (i in 0 until 12) {
                    val angleRad = Math.toRadians((i * 30).toDouble() - 90.0)
                    val outer = Offset(
                        x = center.x + (radius - strokeRing).toFloat() * cos(angleRad).toFloat(),
                        y = center.y + (radius - strokeRing).toFloat() * sin(angleRad).toFloat(),
                    )
                    val inner = Offset(
                        x = center.x + (radius - strokeRing - minorTickLen) * cos(angleRad).toFloat(),
                        y = center.y + (radius - strokeRing - minorTickLen) * sin(angleRad).toFloat(),
                    )
                    drawLine(
                        color = tickColor,
                        start = inner,
                        end = outer,
                        strokeWidth = tickStroke,
                    )
                }

                // Cardinal labels at 0/90/180/270 (N/E/S/W)
                val labelRadius = radius - strokeRing - 22.dp.toPx()
                drawCardinal(
                    textMeasurer = textMeasurer,
                    text = "N",
                    angleDeg = 0f,
                    centerPt = center,
                    radius = labelRadius,
                    style = cardinalBoldStyle,
                )
                drawCardinal(
                    textMeasurer = textMeasurer,
                    text = "E",
                    angleDeg = 90f,
                    centerPt = center,
                    radius = labelRadius,
                    style = cardinalStyle,
                )
                drawCardinal(
                    textMeasurer = textMeasurer,
                    text = "S",
                    angleDeg = 180f,
                    centerPt = center,
                    radius = labelRadius,
                    style = cardinalStyle,
                )
                drawCardinal(
                    textMeasurer = textMeasurer,
                    text = "W",
                    angleDeg = 270f,
                    centerPt = center,
                    radius = labelRadius,
                    style = cardinalStyle,
                )
            }

            // Bearing arrow: triangle pointing INWARD from the edge at
            // (bearingToTarget - currentHeading).
            val arrowAngleDeg = animatedBearing - animatedHeading
            val arrowAngleRad = Math.toRadians(arrowAngleDeg.toDouble() - 90.0)
            val triangleBase = 28.dp.toPx()
            val triangleHeight = 48.dp.toPx()
            val edgeOffset = strokeRing + 6.dp.toPx()

            // Tip: just inside the ring, pointing OUTWARD end of the arrow (nearer to
            // the rim). The triangle "points inward" — its apex is on the ring edge,
            // its base is closer to the center.
            val tip = Offset(
                x = center.x + (radius - edgeOffset) * cos(arrowAngleRad).toFloat(),
                y = center.y + (radius - edgeOffset) * sin(arrowAngleRad).toFloat(),
            )
            val baseCenterRadius = radius - edgeOffset - triangleHeight
            val baseCenter = Offset(
                x = center.x + baseCenterRadius * cos(arrowAngleRad).toFloat(),
                y = center.y + baseCenterRadius * sin(arrowAngleRad).toFloat(),
            )
            // Perpendicular unit vector to spread the triangle base.
            val perpX = -sin(arrowAngleRad).toFloat()
            val perpY = cos(arrowAngleRad).toFloat()
            val half = triangleBase / 2f
            val baseLeft = Offset(baseCenter.x + perpX * half, baseCenter.y + perpY * half)
            val baseRight = Offset(baseCenter.x - perpX * half, baseCenter.y - perpY * half)

            val arrowPath = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(baseLeft.x, baseLeft.y)
                lineTo(baseRight.x, baseRight.y)
                close()
            }
            drawPath(path = arrowPath, color = arrowColor)
        }

        // Center readout — overlaid so we get proper Compose text rendering for
        // the monospace distance label.
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formatDistance(distanceMeters),
                style = MonoCoord.copy(fontSize = 28.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "TO TARGET",
                style = LabelOverline,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCardinal(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    angleDeg: Float,
    centerPt: Offset,
    radius: Float,
    style: TextStyle,
) {
    val angleRad = Math.toRadians(angleDeg.toDouble() - 90.0)
    val px = centerPt.x + radius * cos(angleRad).toFloat()
    val py = centerPt.y + radius * sin(angleRad).toFloat()
    val layout = textMeasurer.measure(text = text, style = style)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            x = px - layout.size.width / 2f,
            y = py - layout.size.height / 2f,
        ),
    )
}

private fun formatDistance(meters: Double?): String = when {
    meters == null -> "—"
    meters < 1.0 -> "${"%.2f".format(meters * 100.0)} cm"
    else -> "${"%.2f".format(meters)} m"
}
