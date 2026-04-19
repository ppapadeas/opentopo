package org.opentopo.app.ui.components.survey

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.opentopo.app.ui.theme.AccuracyGood
import org.opentopo.app.ui.theme.AccuracyGoodDark
import org.opentopo.app.ui.theme.AccuracyOk
import org.opentopo.app.ui.theme.AccuracyOkDark
import org.opentopo.app.ui.theme.AccuracyPoor
import org.opentopo.app.ui.theme.AccuracyPoorDark
import org.opentopo.app.ui.theme.LabelOverline
import org.opentopo.app.ui.theme.MonoDelta

/**
 * EpochAveragingBar — OpenTopo v2.0 averaging-session progress strip.
 *
 * Horizontal status card shown on the Survey bottom sheet while the user is
 * averaging a point across multiple epochs. Renders three rows:
 *
 * 1. Overline title (`EPOCH AVERAGING`) and fraction counter
 *    (`25 / 60`) right-aligned in `MonoDelta`.
 * 2. A [LinearProgressIndicator] whose fill fraction equals
 *    `epochsCollected / epochsTarget` and whose color snaps to the
 *    accuracy ramp based on live σH vs. the configured gate.
 * 3. A footnote with the current σH and the target accuracy gate (hidden
 *    until the first σH estimate is available).
 *
 * The progress fraction animates smoothly with a 300 ms tween so that each
 * captured epoch "pops" the bar forward without jitter.
 *
 * @param epochsCollected Number of epochs averaged so far.
 * @param epochsTarget Target epoch count, as configured by the surveyor.
 * @param currentSigmaH Live σH estimate in meters, or `null` if there aren't
 *  enough samples yet to compute one.
 * @param targetSigmaH Accuracy gate in meters (e.g. `0.02`).
 */
@Composable
fun EpochAveragingBar(
    epochsCollected: Int,
    epochsTarget: Int,
    currentSigmaH: Double?,
    targetSigmaH: Double,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val safeTarget = epochsTarget.coerceAtLeast(1)
    val rawFraction = (epochsCollected.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)

    val animatedFraction by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = tween(durationMillis = 300),
        label = "epochFraction",
    )

    val accuracyColor: Color = when {
        currentSigmaH == null -> MaterialTheme.colorScheme.primary
        currentSigmaH <= targetSigmaH -> if (dark) AccuracyGoodDark else AccuracyGood
        currentSigmaH <= 2.0 * targetSigmaH -> if (dark) AccuracyOkDark else AccuracyOk
        else -> if (dark) AccuracyPoorDark else AccuracyPoor
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "EPOCH AVERAGING",
                    style = LabelOverline,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "$epochsCollected / $epochsTarget",
                    style = MonoDelta,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedFraction },
                color = accuracyColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            )
            if (currentSigmaH != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "σH ${"%.3f".format(currentSigmaH)} m · target ${"%.3f".format(targetSigmaH)} m",
                    style = MonoDelta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
