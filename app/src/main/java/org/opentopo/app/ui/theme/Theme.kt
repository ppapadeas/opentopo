package org.opentopo.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

/** Monospace font for all coordinate and numeric displays. */
val CoordinateFont = FontFamily.Monospace

/** Survey-specific semantic colors accessible via LocalSurveyColors.current. */
data class SurveyColors(
    val rtkFix: Color = FixRtkGreen,
    val rtkFloat: Color = FixFloatOrange,
    val dgps: Color = FixDgpsYellow,
    val gps: Color = FixGpsBlue,
    val noFix: Color = FixNoneRed,
    val onFix: Color = OnFixColor,
    val accuracyGood: Color = AccuracyGood,
    val accuracyOk: Color = AccuracyOk,
    val accuracyPoor: Color = AccuracyPoor,
    val correctionFresh: Color = CorrectionFresh,
    val correctionStale: Color = CorrectionStale,
    val correctionDead: Color = CorrectionDead,
    val stakeoutFar: Color = StakeoutFar,
    val stakeoutClose: Color = StakeoutClose,
    val stakeoutOnPoint: Color = StakeoutOnPoint,
) {
    fun fixColor(quality: Int): Color = when (quality) {
        4 -> rtkFix
        5 -> rtkFloat
        2 -> dgps
        1 -> gps
        else -> noFix
    }

    fun accuracyColor(meters: Double?): Color = when {
        meters == null -> noFix
        meters < 0.02 -> accuracyGood
        meters < 0.05 -> accuracyOk
        else -> accuracyPoor
    }

    fun correctionAgeColor(seconds: Long): Color = when {
        seconds < 2 -> correctionFresh
        seconds < 5 -> correctionStale
        else -> correctionDead
    }

    fun stakeoutColor(distance: Double): Color = when {
        distance < 0.1 -> stakeoutOnPoint
        distance < 1.0 -> stakeoutClose
        else -> stakeoutFar
    }
}

val LocalSurveyColors = staticCompositionLocalOf { SurveyColors() }

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
)

@Composable
fun OpenTopoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(LocalSurveyColors provides SurveyColors()) {
            content()
        }
    }
}
