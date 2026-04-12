package org.opentopo.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.opentopo.app.ui.theme.CoordinateFont
import org.opentopo.app.ui.theme.LocalSurveyColors
import org.opentopo.app.ui.theme.OnSuccessContainer
import org.opentopo.app.ui.theme.OnWarningContainer
import org.opentopo.app.ui.theme.SuccessContainer
import org.opentopo.app.ui.theme.WarningContainer

@Composable
fun FixStatusPill(fixQuality: Int, modifier: Modifier = Modifier) {
    val surveyColors = LocalSurveyColors.current
    val label = surveyColors.fixLabel(fixQuality)

    // Background container color based on fix state
    val containerColor = when (fixQuality) {
        4 -> SuccessContainer
        5 -> WarningContainer
        2 -> WarningContainer
        1 -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (fixQuality) {
        4 -> OnSuccessContainer
        5 -> OnWarningContainer
        2 -> OnWarningContainer
        1 -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onErrorContainer
    }

    // Pulsing dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "fixPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotPulse",
    )

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(percent = 50), // Full pill
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Canvas(Modifier.size(6.dp)) {
                drawCircle(color = contentColor, alpha = alpha)
            }
            Text(
                label,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = CoordinateFont,
            )
        }
    }
}
