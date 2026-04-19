package org.opentopo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * ButtonGroup — OpenTopo v2.0 connected-toggle row.
 *
 * M3 Expressive's replacement for the old `SegmentedButton`. Used wherever a
 * single selection must be made from a small set of peers (connection type
 * `Bluetooth / USB / Internal`, NTRIP preset `HEPOS / CivilPOS / SmartNet / Custom`,
 * survey mode `Point / Line / Polygon`). Visuals:
 *
 * - Full-pill outer surface in `surfaceContainerHigh`, 2 dp inset gap between buttons.
 * - Active item: `primaryContainer` background, `onPrimaryContainer` text.
 * - Inactive: transparent, `onSurfaceVariant` text.
 * - 36 dp height — compact enough to sit inside TonalCards.
 *
 * @param options List of labels, in display order.
 * @param selectedIndex Index of the active item.
 * @param onSelect Invoked with the new index when a button is tapped.
 * @param modifier Passed through to the outer Surface (use `Modifier.fillMaxWidth()`
 *  when the group should occupy a row, or leave `Modifier` default for intrinsic width).
 */
@Composable
fun ButtonGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier,
    ) {
        Row(Modifier.padding(2.dp)) {
            options.forEachIndexed { index, label ->
                val active = index == selectedIndex
                val bg = if (active) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
                val fg = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .height(36.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(bg)
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = if (active) MaterialTheme.typography.labelLargeEmphasized else MaterialTheme.typography.labelLarge,
                        color = fg,
                    )
                }
            }
        }
    }
}
