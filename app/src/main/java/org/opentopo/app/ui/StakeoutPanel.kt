package org.opentopo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.opentopo.app.survey.Stakeout
import org.opentopo.app.survey.StakeoutTarget
import org.opentopo.app.ui.theme.CoordinateFont
import org.opentopo.app.ui.theme.LocalSurveyColors

@Composable
fun StakeoutPanel(
    stakeout: Stakeout?,
    modifier: Modifier = Modifier,
) {
    val surveyColors = LocalSurveyColors.current
    val result by stakeout?.result?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
    var targetName by remember { mutableStateOf("") }
    var targetE by remember { mutableStateOf("") }
    var targetN by remember { mutableStateOf("") }
    var hasTarget by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("Stakeout", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        if (!hasTarget) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Target (EGSA87)", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = targetName, onValueChange = { targetName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = targetE, onValueChange = { targetE = it }, label = { Text("E (m)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = targetN, onValueChange = { targetN = it }, label = { Text("N (m)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val e = targetE.toDoubleOrNull(); val n = targetN.toDoubleOrNull()
                            if (e != null && n != null) { stakeout?.setTarget(StakeoutTarget(targetName.ifBlank { "Target" }, e, n)); hasTarget = true }
                        },
                        enabled = targetE.toDoubleOrNull() != null && targetN.toDoubleOrNull() != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Start") }
                }
            }
        } else {
            val r = result
            if (r == null) {
                Text("Waiting for fix...", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            } else {
                val distColor = surveyColors.stakeoutColor(r.distance)

                // Compass arrow
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    StakeoutArrow(bearingDeg = r.bearingDeg, distance = r.distance)
                    Spacer(Modifier.height(8.dp))
                    Text("%.3f m".format(r.distance), style = MaterialTheme.typography.displayMedium, fontFamily = CoordinateFont, color = distColor, fontWeight = FontWeight.Bold)
                    Text("${r.bearingCardinal} (%.1f\u00B0)".format(r.bearingDeg), style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp)) {
                        SurveyStatusRow("\u0394E", "%.3f m".format(r.deltaEasting))
                        SurveyStatusRow("\u0394N", "%.3f m".format(r.deltaNorthing))
                        SurveyStatusRow("Target", r.target.name)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { stakeout?.setTarget(null); hasTarget = false }, modifier = Modifier.fillMaxWidth()) { Text("Clear Target") }
        }
    }
}

@Composable
private fun StakeoutArrow(bearingDeg: Double, distance: Double) {
    val surveyColors = LocalSurveyColors.current
    val color = surveyColors.stakeoutColor(distance)

    Canvas(modifier = Modifier.size(160.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Outer circle
        drawCircle(color = color.copy(alpha = 0.1f), radius = radius, center = center)
        drawCircle(color = color, radius = radius, center = center, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        // N/S/E/W tick marks
        val tickLen = 8.dp.toPx()
        for (deg in listOf(0f, 90f, 180f, 270f)) {
            rotate(deg, pivot = center) {
                drawLine(color.copy(alpha = 0.4f), Offset(center.x, center.y - radius + 2.dp.toPx()), Offset(center.x, center.y - radius + tickLen + 2.dp.toPx()), strokeWidth = 1.5f.dp.toPx())
            }
        }

        // Arrow pointing in bearing direction
        rotate(bearingDeg.toFloat(), pivot = center) {
            val arrowPath = Path().apply {
                moveTo(center.x, center.y - radius * 0.7f)                    // tip
                lineTo(center.x - 14.dp.toPx(), center.y - radius * 0.1f)     // left
                lineTo(center.x, center.y - radius * 0.25f)                   // notch
                lineTo(center.x + 14.dp.toPx(), center.y - radius * 0.1f)     // right
                close()
            }
            drawPath(arrowPath, color)
        }
    }
}
