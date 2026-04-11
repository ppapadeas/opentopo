package org.opentopo.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Survey-specific semantic colors accessible via LocalSurveyColors.current. */
data class SurveyColors(
    val rtkFix: Color,
    val rtkFloat: Color,
    val dgps: Color,
    val gps: Color,
    val noFix: Color,
    val onFix: Color,
    val accuracyGood: Color,
    val accuracyOk: Color,
    val accuracyPoor: Color,
    val correctionFresh: Color,
    val correctionStale: Color,
    val correctionDead: Color,
    val stakeoutFar: Color,
    val stakeoutClose: Color,
    val stakeoutOnPoint: Color,
    val recordingActive: Color,
    val recordingProgress: Color,
) {
    fun fixColor(quality: Int): Color = when (quality) {
        4 -> rtkFix
        5 -> rtkFloat
        2 -> dgps
        1 -> gps
        else -> noFix
    }

    fun fixLabel(quality: Int): String = when (quality) {
        4 -> "RTK Fix"
        5 -> "RTK Float"
        2 -> "DGPS"
        1 -> "GPS"
        else -> "No Fix"
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

val LightSurveyColors = SurveyColors(
    rtkFix = FixRtkGreen,
    rtkFloat = FixFloatOrange,
    dgps = FixDgpsYellow,
    gps = FixGpsBlue,
    noFix = FixNoneRed,
    onFix = OnFixColor,
    accuracyGood = AccuracyGood,
    accuracyOk = AccuracyOk,
    accuracyPoor = AccuracyPoor,
    correctionFresh = CorrectionFresh,
    correctionStale = CorrectionStale,
    correctionDead = CorrectionDead,
    stakeoutFar = StakeoutFar,
    stakeoutClose = StakeoutClose,
    stakeoutOnPoint = StakeoutOnPoint,
    recordingActive = RecordingActive,
    recordingProgress = RecordingProgress,
)

val DarkSurveyColors = SurveyColors(
    rtkFix = FixRtkGreenDark,
    rtkFloat = FixFloatOrangeDark,
    dgps = FixDgpsYellowDark,
    gps = FixGpsBlueDark,
    noFix = FixNoneRedDark,
    onFix = OnFixColor,
    accuracyGood = AccuracyGoodDark,
    accuracyOk = AccuracyOkDark,
    accuracyPoor = AccuracyPoorDark,
    correctionFresh = CorrectionFreshDark,
    correctionStale = CorrectionStaleDark,
    correctionDead = CorrectionDeadDark,
    stakeoutFar = StakeoutFarDark,
    stakeoutClose = StakeoutCloseDark,
    stakeoutOnPoint = StakeoutOnPointDark,
    recordingActive = RecordingActiveDark,
    recordingProgress = RecordingProgressDark,
)

val LocalSurveyColors = staticCompositionLocalOf { LightSurveyColors }

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    scrim = md_theme_light_scrim,
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    scrim = md_theme_dark_scrim,
)

private val OpenTopoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    val surveyColors = if (darkTheme) DarkSurveyColors else LightSurveyColors

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = OpenTopoTypography,
        shapes = OpenTopoShapes,
        motionScheme = MotionScheme.expressive(),
    ) {
        CompositionLocalProvider(LocalSurveyColors provides surveyColors) {
            content()
        }
    }
}
