package org.opentopo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.opentopo.app.survey.Stakeout
import org.opentopo.app.survey.StakeoutTarget

@Composable
fun StakeoutPanel(
    stakeout: Stakeout?,
    modifier: Modifier = Modifier,
) {
    val result by stakeout?.result?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }

    var targetName by remember { mutableStateOf("") }
    var targetE by remember { mutableStateOf("") }
    var targetN by remember { mutableStateOf("") }
    var hasTarget by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("Stakeout", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        if (!hasTarget) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
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
                            if (e != null && n != null) {
                                stakeout?.setTarget(StakeoutTarget(targetName.ifBlank { "Target" }, e, n))
                                hasTarget = true
                            }
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
                Text("%.2f m".format(r.distance), style = MaterialTheme.typography.displayMedium, fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("${r.bearingCardinal} (%.1f\u00B0)".format(r.bearingDeg), style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))

                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp)) {
                        DeltaRow("\u0394E", "%.3f m".format(r.deltaEasting))
                        DeltaRow("\u0394N", "%.3f m".format(r.deltaNorthing))
                        DeltaRow("Target", "${r.target.name}")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { stakeout?.setTarget(null); hasTarget = false },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Clear Target") }
        }
    }
}

@Composable
private fun DeltaRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}
