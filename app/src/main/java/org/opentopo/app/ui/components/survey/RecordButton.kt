package org.opentopo.app.ui.components.survey

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material3.toPath
import androidx.graphics.shapes.Morph
import org.opentopo.app.ui.theme.RecordingActive
import org.opentopo.app.ui.theme.RecordingActiveDark
import org.opentopo.app.ui.theme.RecordingProgress
import org.opentopo.app.ui.theme.RecordingProgressDark

/**
 * RecordButton — OpenTopo v2.0 capture-epoch button.
 *
 * An 84 dp circular record control whose outline morphs between the expressive
 * `MaterialShapes.Cookie9Sided` (idle) and `MaterialShapes.Circle` (pressed).
 * Used in:
 *
 * - **Survey** screen to capture a single averaged epoch at the current
 *   position (primary button of the split-button row);
 * - **Stakeout** screen, where tapping records the as-staked point after the
 *   user has navigated within the configured on-point radius.
 *
 * The button goes disabled until the current fix passes the accuracy gate so
 * that surveyors cannot record a noisy epoch. When [isRecording] is true, a
 * thin progress ring sweeps clockwise around the edge, tracing [progress].
 *
 * @param onClick Invoked on tap when [enabled] is true.
 * @param enabled `false` while the current fix fails the accuracy gate — the
 *  button dims to `surfaceContainerHigh` and becomes unclickable.
 * @param isRecording `true` while an averaging session is in progress; draws
 *  the progress ring and keeps the pressed-morph shape.
 * @param progress 0f..1f — driven by the epoch averaging state (epochs
 *  collected / target).
 * @param contentDescription Talkback label, defaults to `"Record epoch"`.
 */
@Composable
fun RecordButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isRecording: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    contentDescription: String = "Record epoch",
) {
    val dark = isSystemInDarkTheme()

    // OpenTopo v2 mockup specifies the Cookie9Sided record shape in the
    // primary teal ramp (#1c6e5a). Keep the legacy RecordingActive tokens as
    // the dark fallback so existing theme work is preserved.
    val primaryContainer = MaterialTheme.colorScheme.primary
    val background: Color = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
        dark -> RecordingActiveDark
        else -> primaryContainer
    }
    val iconTint: Color = if (enabled) {
        if (dark) Color(0xFF3A0B1A) else Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val progressColor: Color = if (dark) RecordingProgressDark else RecordingProgress
    val haloColor: Color = if (dark) RecordingProgressDark else primaryContainer

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 0f = Cookie9Sided (idle), 1f = Circle (pressed/recording)
    val morphTarget = if (isPressed || isRecording) 1f else 0f
    val morphProgress by animateFloatAsState(
        targetValue = morphTarget,
        animationSpec = tween(durationMillis = 220),
        label = "recordMorph",
    )

    // Build the Morph once; feed its animated path into a Shape on every frame.
    val morph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Circle) }
    val morphShape = remember(morphProgress) { MorphShape(morph, morphProgress) }

    val sweep by animateFloatAsState(
        targetValue = (progress.coerceIn(0f, 1f)) * 360f,
        animationSpec = tween(durationMillis = 250),
        label = "recordSweep",
    )

    // Ring-pulse overlay for averaging state (matches mockup ringpulse keyframes:
    // scale 0.85 → 1.45, opacity 0.7 → 0, linear 1.6s loop).
    val ringTransition = rememberInfiniteTransition(label = "recordRingPulse")
    val ringT by ringTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringT",
    )
    val ringScale = 0.85f + ringT * (1.45f - 0.85f)
    val ringAlpha = (0.7f * (1f - ringT)).coerceIn(0f, 0.7f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // Soft pulsing halo ring — behind the morph surface, only while recording.
        if (isRecording) {
            Canvas(
                Modifier
                    .size(size)
                    .drawBehind { },
            ) {
                val radius = (this.size.minDimension / 2f) * ringScale
                drawCircle(
                    color = haloColor,
                    radius = radius,
                    center = center,
                    alpha = ringAlpha,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        Surface(
            color = background,
            shape = morphShape,
            modifier = Modifier
                .size(size)
                .clip(morphShape)
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = ripple(),
                ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Progress ring — drawn just inside the outer edge when recording.
                if (isRecording) {
                    Canvas(Modifier.size(size)) {
                        val inset = 6.dp.toPx()
                        val strokeWidth = 3.dp.toPx()
                        drawArc(
                            color = progressColor,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(this.size.width - inset * 2, this.size.height - inset * 2),
                            style = Stroke(width = strokeWidth),
                        )
                    }
                }
                if (isRecording) {
                    // Stop-square overlay (28dp rounded 4dp) — the visual stop cue
                    // specified by the mockup in recording state.
                    Canvas(Modifier.size(28.dp)) {
                        drawRoundRect(
                            color = Color.White,
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.RadioButtonChecked,
                        contentDescription = contentDescription,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

/**
 * Thin [Shape] wrapper around an animated [Morph] so that [Surface] and
 * [Modifier.clip] can pick up the cookie↔circle interpolation each frame.
 */
private class MorphShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val out = morph.toPath(progress = progress)
        // Scale unit-square normalized shape into the available size.
        val scale = androidx.compose.ui.graphics.Matrix()
        scale.scale(x = size.width, y = size.height)
        out.transform(scale)
        // Re-center in case the centroid sits off the geometric midpoint.
        val bounds = out.getBounds()
        val dx = (size.width - bounds.width) / 2f - bounds.left
        val dy = (size.height - bounds.height) / 2f - bounds.top
        out.translate(Offset(dx, dy))
        return Outline.Generic(out)
    }
}
