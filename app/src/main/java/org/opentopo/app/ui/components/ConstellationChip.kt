package org.opentopo.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.opentopo.app.gnss.Constellation
import org.opentopo.app.ui.theme.ConstellationColors
import org.opentopo.app.ui.theme.CoordinateFont

@Composable
fun ConstellationChip(constellation: Constellation, count: Int, modifier: Modifier = Modifier) {
    val (bg, fg) = when (constellation) {
        Constellation.GPS -> ConstellationColors.gpsContainer to ConstellationColors.gps
        Constellation.GLONASS -> ConstellationColors.glonassContainer to ConstellationColors.glonass
        Constellation.GALILEO -> ConstellationColors.galileoContainer to ConstellationColors.galileo
        Constellation.BEIDOU -> ConstellationColors.beidouContainer to ConstellationColors.beidou
        else -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
    }
    val label = when (constellation) {
        Constellation.GPS -> "GPS"
        Constellation.GLONASS -> "GLO"
        Constellation.GALILEO -> "GAL"
        Constellation.BEIDOU -> "BDS"
        else -> "?"
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            "$label $count",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = CoordinateFont,
        )
    }
}
