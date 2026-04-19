package org.opentopo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.opentopo.app.ui.theme.LabelOverline
import org.opentopo.app.ui.theme.MonoCoord
import org.opentopo.app.ui.theme.MonoDelta

/**
 * CoordinateBlock — OpenTopo v2.0 atomic coordinate display.
 *
 * One-line version (legacy): pass [easting] and [northing], nothing else — renders
 * an E/N row with the optional label above in overline style.
 *
 * v2 stacked version: use [CoordinateBlock(label, easting, northing, height, fixQuality, sigmaH, ...)]
 * to render the design prototype's EGSA87 card — overline header, monospace E/N/H
 * stacked with line-height 1.45, and an optional footer row with the fix pill
 * plus σH/σV values in `MonoDelta`.
 */
@Composable
fun CoordinateBlock(
    label: String,
    easting: String,
    northing: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                label.uppercase(),
                style = LabelOverline,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("E  $easting", style = MonoCoord, fontWeight = FontWeight.W500)
                Text("N  $northing", style = MonoCoord, fontWeight = FontWeight.W500)
            }
        }
    }
}

/**
 * v2 stacked CoordinateBlock with height, fix pill, and σ footer.
 *
 * @param label Overline label (e.g. `"EGSA87 · EPSG 2100"`).
 * @param easting E value, already formatted.
 * @param northing N value, already formatted.
 * @param height Optional orthometric height string with unit (e.g. `"114.082 m"`).
 *  When present, rendered as a third muted row.
 * @param fixQuality If non-null, a compact [FixStatusPill] is drawn in the footer.
 * @param sigmaH Optional σH formatted with unit (e.g. `"σH 0.009 m"`) — shown in
 *  the footer next to the fix pill.
 */
@Composable
fun CoordinateBlock(
    label: String,
    easting: String,
    northing: String,
    height: String?,
    modifier: Modifier = Modifier,
    fixQuality: Int? = null,
    sigmaH: String? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                label.uppercase(),
                style = LabelOverline,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text("E  $easting", style = MonoCoord)
            Text("N  $northing", style = MonoCoord)
            if (!height.isNullOrBlank()) {
                Text(
                    "H  $height",
                    style = MonoCoord,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (fixQuality != null || !sigmaH.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (fixQuality != null) {
                        FixStatusPill(fixQuality = fixQuality)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (!sigmaH.isNullOrBlank()) {
                        Text(
                            sigmaH,
                            style = MonoDelta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
