package org.opentopo.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── M3 Light scheme (seed: #006B5E teal-green) ──

val md_theme_light_primary = Color(0xFF006B5E)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFF6FF7DD)
val md_theme_light_onPrimaryContainer = Color(0xFF00201B)
val md_theme_light_secondary = Color(0xFF4A635C)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFCCE8DF)
val md_theme_light_onSecondaryContainer = Color(0xFF06201A)
val md_theme_light_tertiary = Color(0xFF436278)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFC7E7FF)
val md_theme_light_onTertiaryContainer = Color(0xFF001E2E)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFF5FBF7)
val md_theme_light_onBackground = Color(0xFF171D1B)
val md_theme_light_surface = Color(0xFFF5FBF7)
val md_theme_light_onSurface = Color(0xFF171D1B)
val md_theme_light_surfaceVariant = Color(0xFFDAE5E0)
val md_theme_light_onSurfaceVariant = Color(0xFF3F4945)
val md_theme_light_outline = Color(0xFF6F7975)
val md_theme_light_outlineVariant = Color(0xFFBFC9C4)
val md_theme_light_inverseSurface = Color(0xFF2B322F)
val md_theme_light_inverseOnSurface = Color(0xFFECF2EE)
val md_theme_light_inversePrimary = Color(0xFF50DBC2)
val md_theme_light_surfaceTint = Color(0xFF006B5E)
val md_theme_light_scrim = Color(0xFF000000)

// ── M3 Dark scheme ──

val md_theme_dark_primary = Color(0xFF50DBC2)
val md_theme_dark_onPrimary = Color(0xFF003830)
val md_theme_dark_primaryContainer = Color(0xFF005046)
val md_theme_dark_onPrimaryContainer = Color(0xFF6FF7DD)
val md_theme_dark_secondary = Color(0xFFB1CCC3)
val md_theme_dark_onSecondary = Color(0xFF1C352F)
val md_theme_dark_secondaryContainer = Color(0xFF334B45)
val md_theme_dark_onSecondaryContainer = Color(0xFFCCE8DF)
val md_theme_dark_tertiary = Color(0xFFAACAE4)
val md_theme_dark_onTertiary = Color(0xFF113348)
val md_theme_dark_tertiaryContainer = Color(0xFF2B4A5F)
val md_theme_dark_onTertiaryContainer = Color(0xFFC7E7FF)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF0F1512)
val md_theme_dark_onBackground = Color(0xFFDEE4E0)
val md_theme_dark_surface = Color(0xFF0F1512)
val md_theme_dark_onSurface = Color(0xFFDEE4E0)
val md_theme_dark_surfaceVariant = Color(0xFF3F4945)
val md_theme_dark_onSurfaceVariant = Color(0xFFBFC9C4)
val md_theme_dark_outline = Color(0xFF89938F)
val md_theme_dark_outlineVariant = Color(0xFF3F4945)
val md_theme_dark_inverseSurface = Color(0xFFDEE4E0)
val md_theme_dark_inverseOnSurface = Color(0xFF2B322F)
val md_theme_dark_inversePrimary = Color(0xFF006B5E)
val md_theme_dark_surfaceTint = Color(0xFF50DBC2)
val md_theme_dark_scrim = Color(0xFF000000)

// ── Fix quality colors (light) ──
val FixRtkGreen = Color(0xFF1B6D2F)       // success
val FixFloatOrange = Color(0xFF7B5800)    // warning
val FixDgpsYellow = Color(0xFFF9A825)     // distinct bright yellow
val FixGpsBlue = Color(0xFF1565C0)
val FixNoneRed = Color(0xFFBA1A1A)        // error
val OnFixColor = Color(0xFFFFFFFF)

// ── Fix quality colors (dark) ──
val FixRtkGreenDark = Color(0xFFA1F5A3)
val FixFloatOrangeDark = Color(0xFFFFDEA6)
val FixDgpsYellowDark = Color(0xFFFFD54F)
val FixGpsBlueDark = Color(0xFF90CAF9)
val FixNoneRedDark = Color(0xFFFFB4AB)

// ── Containers for fix states (light) ──
val SuccessContainer = Color(0xFFA1F5A3)
val OnSuccessContainer = Color(0xFF002108)
val WarningContainer = Color(0xFFFFDEA6)
val OnWarningContainer = Color(0xFF271900)

// ── Accuracy thresholds ──
val AccuracyGood = Color(0xFF1B6D2F)
val AccuracyOk = Color(0xFF7B5800)
val AccuracyPoor = Color(0xFFBA1A1A)
val AccuracyGoodDark = Color(0xFFA1F5A3)
val AccuracyOkDark = Color(0xFFFFDEA6)
val AccuracyPoorDark = Color(0xFFFFB4AB)

// ── Correction age ──
val CorrectionFresh = Color(0xFF1B6D2F)
val CorrectionStale = Color(0xFF7B5800)
val CorrectionDead = Color(0xFFBA1A1A)
val CorrectionFreshDark = Color(0xFFA1F5A3)
val CorrectionStaleDark = Color(0xFFFFDEA6)
val CorrectionDeadDark = Color(0xFFFFB4AB)

// ── Stakeout ──
val StakeoutFar = Color(0xFFBA1A1A)
val StakeoutClose = Color(0xFF7B5800)
val StakeoutOnPoint = Color(0xFF1B6D2F)
val StakeoutFarDark = Color(0xFFFFB4AB)
val StakeoutCloseDark = Color(0xFFFFDEA6)
val StakeoutOnPointDark = Color(0xFFA1F5A3)

// ── Recording ──
val RecordingActive = Color(0xFFD1416A)
val RecordingProgress = Color(0xFF006B5E)
val RecordingActiveDark = Color(0xFFFFD9E0)
val RecordingProgressDark = Color(0xFF50DBC2)

// ── Constellation colors (categorical) ──
object ConstellationColors {
    val gps = Color(0xFF558B2F)
    val gpsContainer = Color(0xFFDCEDC8)
    val glonass = Color(0xFF1565C0)
    val glonassContainer = Color(0xFFE3F2FD)
    val galileo = Color(0xFF6A1B9A)
    val galileoContainer = Color(0xFFF3E5F5)
    val beidou = Color(0xFFE65100)
    val beidouContainer = Color(0xFFFFF3E0)
}
