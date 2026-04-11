package org.opentopo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.opentopo.app.gnss.Constellation
import org.opentopo.app.gnss.SatelliteInfo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Skyplot — polar chart showing satellite positions.
 *
 * Elevation 90° at center, 0° at rim. Azimuth 0° (North) at top, clockwise.
 * Satellite dots colored by constellation, sized by SNR.
 */
@Composable
fun Skyplot(
    satellites: List<SatelliteInfo>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val bgColor = MaterialTheme.colorScheme.surfaceContainerLow

    Canvas(modifier = modifier.size(220.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = size.minDimension / 2 - 16.dp.toPx()

        // Background
        drawCircle(bgColor, radius + 8.dp.toPx(), Offset(cx, cy))

        // Elevation rings at 0°, 30°, 60° (90° is center)
        val ringStroke = Stroke(width = 1.dp.toPx())
        for (elev in listOf(0, 30, 60)) {
            val r = radius * (90 - elev) / 90f
            drawCircle(gridColor, r, Offset(cx, cy), style = ringStroke)
        }

        // Cross-hairs (N-S, E-W)
        drawLine(gridColor, Offset(cx, cy - radius), Offset(cx, cy + radius), strokeWidth = 0.5.dp.toPx())
        drawLine(gridColor, Offset(cx - radius, cy), Offset(cx + radius, cy), strokeWidth = 0.5.dp.toPx())

        // Cardinal labels
        val labelStyle = TextStyle(
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        for ((label, angle) in listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)) {
            val rad = angle * PI.toFloat() / 180f - PI.toFloat() / 2
            val lx = cx + (radius + 10.dp.toPx()) * cos(rad)
            val ly = cy + (radius + 10.dp.toPx()) * sin(rad)
            val measured = textMeasurer.measure(label, labelStyle)
            drawText(
                measured,
                topLeft = Offset(lx - measured.size.width / 2, ly - measured.size.height / 2),
            )
        }

        // Elevation labels
        val elevStyle = TextStyle(color = gridColor, fontSize = 8.sp)
        for (elev in listOf(30, 60)) {
            val r = radius * (90 - elev) / 90f
            val measured = textMeasurer.measure("${elev}°", elevStyle)
            drawText(
                measured,
                topLeft = Offset(cx + 2.dp.toPx(), cy - r - measured.size.height),
            )
        }

        // Satellite dots
        for (sat in satellites) {
            val elev = sat.elevationDeg ?: continue
            val azim = sat.azimuthDeg ?: continue
            if (elev < 0 || elev > 90) continue

            val r = radius * (90 - elev) / 90f
            val azRad = (azim - 90f) * PI.toFloat() / 180f
            val sx = cx + r * cos(azRad)
            val sy = cy + r * sin(azRad)

            val color = constellationColor(sat.constellation)
            val snr = sat.snrDb ?: 0
            val dotRadius = when {
                snr >= 35 -> 6.dp.toPx()
                snr >= 20 -> 4.5.dp.toPx()
                snr > 0 -> 3.dp.toPx()
                else -> 2.5.dp.toPx()
            }
            val alpha = when {
                snr >= 30 -> 1f
                snr >= 15 -> 0.7f
                snr > 0 -> 0.4f
                else -> 0.25f
            }

            // Dot
            drawCircle(color.copy(alpha = alpha), dotRadius, Offset(sx, sy))
            drawCircle(Color.White, dotRadius, Offset(sx, sy), style = Stroke(width = 0.8.dp.toPx()))

            // PRN label
            val prnStyle = TextStyle(color = color.copy(alpha = alpha), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            val prnText = textMeasurer.measure("${sat.prn}", prnStyle)
            drawText(prnText, topLeft = Offset(sx - prnText.size.width / 2, sy + dotRadius + 1.dp.toPx()))
        }
    }
}

private fun constellationColor(c: Constellation): Color = when (c) {
    Constellation.GPS -> Color(0xFF1565C0)       // Blue
    Constellation.GLONASS -> Color(0xFFC62828)    // Red
    Constellation.GALILEO -> Color(0xFF2E7D32)    // Green
    Constellation.BEIDOU -> Color(0xFFEF6C00)     // Orange
    Constellation.UNKNOWN -> Color(0xFF757575)    // Gray
}
