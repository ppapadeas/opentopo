package org.opentopo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.opentopo.app.survey.TrigPoint
import org.opentopo.app.ui.theme.CoordinateFont

/**
 * v2 Trig panel — simple scroll list of cached trig points surfaced through the
 * map tap bubble. Selecting an entry opens the existing `VerificationDialog`-style
 * trig point detail AlertDialog.
 */
@Composable
internal fun TrigPanel(
    trigPoints: List<TrigPoint>,
    onSelect: (TrigPoint) -> Unit,
) {
    if (trigPoints.isEmpty()) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Pan the map to load nearby GYS trig points.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val sorted = remember(trigPoints) {
        trigPoints.sortedWith(
            compareBy(nullsLast<Double>()) { it.distanceM },
        )
    }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        sorted.forEach { tp ->
            TrigListItem(tp, onSelect)
        }
    }
}

@Composable
private fun TrigListItem(tp: TrigPoint, onSelect: (TrigPoint) -> Unit) {
    Surface(
        onClick = { onSelect(tp) },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.PinDrop,
                contentDescription = null,
                tint = when (tp.status) {
                    "OK" -> Color(0xFF4CAF50)
                    "DAMAGED" -> Color(0xFFFF9800)
                    "DESTROYED" -> Color(0xFFF44336)
                    "MISSING" -> Color(0xFF9C27B0)
                    else -> Color(0xFF9E9E9E)
                },
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "GYS ${tp.gysId}",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = CoordinateFont,
                    fontWeight = FontWeight.Bold,
                )
                tp.name?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            tp.distanceM?.let {
                Text(
                    "${"%.0f".format(it)} m",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = CoordinateFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
