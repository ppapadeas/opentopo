@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package org.opentopo.app.ui.components.survey

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * SplitButton — OpenTopo v2.0 Material 3 Expressive split button.
 *
 * Two tonal buttons fused into a pill with a 2 dp hairline gap. Used on the
 * Survey screen so the primary action (`RECORD EPOCH`) stays large and
 * thumb-friendly while a smaller "chevron chip" exposes the current
 * accuracy-gate preset (e.g. `±2 cm`) and opens a dropdown for picking a
 * different gate.
 *
 * Caller supplies the dropdown UX — this composable only owns the two
 * clickable buttons and leaves state to the parent.
 *
 * @param primaryLabel Text on the left button (typically uppercase command
 *  verb like `"RECORD EPOCH"`).
 * @param onPrimaryClick Invoked when the main button is tapped.
 * @param secondaryLabel Compact summary of the dropdown state (e.g. `"±2 cm"`).
 * @param onSecondaryClick Invoked when the chevron chip is tapped — the
 *  caller should open its own dropdown/bottom sheet.
 * @param enabled When false, both buttons dim and stop responding to taps.
 */
@Composable
fun SplitButton(
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    secondaryLabel: String,
    onSecondaryClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val primaryShape = RoundedCornerShape(
        topStart = 24.dp,
        bottomStart = 24.dp,
        topEnd = 6.dp,
        bottomEnd = 6.dp,
    )
    val secondaryShape = RoundedCornerShape(
        topStart = 6.dp,
        bottomStart = 6.dp,
        topEnd = 24.dp,
        bottomEnd = 24.dp,
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = onPrimaryClick,
            enabled = enabled,
            shape = primaryShape,
            modifier = Modifier
                .weight(1f, fill = true)
                .height(48.dp),
        ) {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(2.dp))
        FilledTonalButton(
            onClick = onSecondaryClick,
            enabled = enabled,
            shape = secondaryShape,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 12.dp,
                vertical = 0.dp,
            ),
            modifier = Modifier
                .width(80.dp)
                .height(48.dp),
        ) {
            Text(
                text = secondaryLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
