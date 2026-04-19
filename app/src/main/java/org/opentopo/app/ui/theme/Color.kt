package org.opentopo.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
//  OpenTopo v2.0 palette — Material 3 Expressive / SPEC_2025 tonal scales.
//
//  Seeded from the OpenTopo mark (deep Greek pine-teal) and regenerated through
//  the SPEC_2025 pipeline. Tertiary picks up the HEPOS grid's Ktimatologio
//  orange to flag corrections, transforms, and datum shifts.
// ─────────────────────────────────────────────────────────────────────────────

// ── Light scheme ──
val md_theme_light_primary = Color(0xFF1C6E5A)           // pine teal (40)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFA5F2D9)
val md_theme_light_onPrimaryContainer = Color(0xFF00261C)
val md_theme_light_secondary = Color(0xFF4C6359)         // river stone (40)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFCFE9DB)
val md_theme_light_onSecondaryContainer = Color(0xFF0A2017)
val md_theme_light_tertiary = Color(0xFFB0522C)          // HEPOS ochre (40)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFFFDBC9)
val md_theme_light_onTertiaryContainer = Color(0xFF3A1100)
val md_theme_light_error = Color(0xFFB3261E)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFF9DEDC)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFF5FAF7)
val md_theme_light_onBackground = Color(0xFF161D1A)
val md_theme_light_surface = Color(0xFFF5FAF7)
val md_theme_light_onSurface = Color(0xFF161D1A)
val md_theme_light_surfaceVariant = Color(0xFFDAE5E0)
val md_theme_light_onSurfaceVariant = Color(0xFF3F4945)
val md_theme_light_outline = Color(0xFF6F7974)
val md_theme_light_outlineVariant = Color(0xFFBFC9C4)
val md_theme_light_inverseSurface = Color(0xFF2B322F)
val md_theme_light_inverseOnSurface = Color(0xFFECF2EE)
val md_theme_light_inversePrimary = Color(0xFF4A9A84)    // primary-60
val md_theme_light_surfaceTint = Color(0xFF1C6E5A)
val md_theme_light_scrim = Color(0xFF000000)

// Light surface containers — SPEC_2025 warm-green undertone
val md_theme_light_surfaceContainerLowest = Color(0xFFFFFFFF)
val md_theme_light_surfaceContainerLow = Color(0xFFEFF5F1)
val md_theme_light_surfaceContainer = Color(0xFFE9EFEC)
val md_theme_light_surfaceContainerHigh = Color(0xFFE3EAE6)
val md_theme_light_surfaceContainerHighest = Color(0xFFDEE4E0)
val md_theme_light_surfaceDim = Color(0xFFD6DCD8)
val md_theme_light_surfaceBright = Color(0xFFFCFFFC)

// ── Dark scheme ──
val md_theme_dark_primary = Color(0xFF4A9A84)            // primary-60 for dark
val md_theme_dark_onPrimary = Color(0xFF00261C)
val md_theme_dark_primaryContainer = Color(0xFF00493D)
val md_theme_dark_onPrimaryContainer = Color(0xFFA5F2D9)
val md_theme_dark_secondary = Color(0xFFB3CABF)
val md_theme_dark_onSecondary = Color(0xFF1E3529)
val md_theme_dark_secondaryContainer = Color(0xFF33493F)
val md_theme_dark_onSecondaryContainer = Color(0xFFCFE9DB)
val md_theme_dark_tertiary = Color(0xFFFFB693)
val md_theme_dark_onTertiary = Color(0xFF561F00)
val md_theme_dark_tertiaryContainer = Color(0xFF7C3512)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFDBC9)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF0F1716)
val md_theme_dark_onBackground = Color(0xFFDEE4E0)
val md_theme_dark_surface = Color(0xFF0F1716)
val md_theme_dark_onSurface = Color(0xFFDEE4E0)
val md_theme_dark_surfaceVariant = Color(0xFF3F4945)
val md_theme_dark_onSurfaceVariant = Color(0xFFBFC9C4)
val md_theme_dark_outline = Color(0xFF89938F)
val md_theme_dark_outlineVariant = Color(0xFF3F4945)
val md_theme_dark_inverseSurface = Color(0xFF243530)
val md_theme_dark_inverseOnSurface = Color(0xFFDEE4E0)
val md_theme_dark_inversePrimary = Color(0xFF1C6E5A)
val md_theme_dark_surfaceTint = Color(0xFF4A9A84)
val md_theme_dark_scrim = Color(0xFF000000)

// Dark surface containers — AMOLED-aware stepping
val md_theme_dark_surfaceContainerLowest = Color(0xFF000000)
val md_theme_dark_surfaceContainerLow = Color(0xFF0A0A0A)
val md_theme_dark_surfaceContainer = Color(0xFF0E1513)
val md_theme_dark_surfaceContainerHigh = Color(0xFF151C1A)
val md_theme_dark_surfaceContainerHighest = Color(0xFF1A2422)
val md_theme_dark_surfaceDim = Color(0xFF0F1716)
val md_theme_dark_surfaceBright = Color(0xFF303633)

// ─────────────────────────────────────────────────────────────────────────────
//  Domain semantic — fix quality ramp.
//
//  Fix-quality color is the single most important signal in the app. It takes
//  a dedicated semantic ramp (NOT borrowed from error/warning/success) because
//  surveyors learn these five colors by muscle memory.
// ─────────────────────────────────────────────────────────────────────────────

// Solid ramp (light) — used on dots, pills, markers, convergence rings
val FixRtkGreen = Color(0xFF1C6E5A)       // RTK Fix · Q=4  · deep teal
val FixFloatOrange = Color(0xFFB0A300)    // RTK Float · Q=5 · amber
val FixDgpsYellow = Color(0xFF4A73C4)     // DGPS · Q=2 · blue (historic name kept)
val FixGpsBlue = Color(0xFF707A75)        // GPS · Q=1 · neutral (historic name kept)
val FixNoneRed = Color(0xFF9B3A2E)        // No Fix · red
val OnFixColor = Color(0xFFFFFFFF)

// Solid ramp (dark) — slightly brighter for contrast on dark surfaces
val FixRtkGreenDark = Color(0xFFA5F2D9)
val FixFloatOrangeDark = Color(0xFFFDF0B3)
val FixDgpsYellowDark = Color(0xFFD6E3FF)
val FixGpsBlueDark = Color(0xFFDCE5E0)
val FixNoneRedDark = Color(0xFFFFD9D2)

// Tonal containers for pill backgrounds — the actual v2 pill styling
val RtkPillContainer = Color(0xFFA5F2D9)       // rtk bg
val RtkPillOn = Color(0xFF004D3B)              // rtk text
val FloatPillContainer = Color(0xFFFDF0B3)
val FloatPillOn = Color(0xFF4A3F00)
val DgpsPillContainer = Color(0xFFD6E3FF)
val DgpsPillOn = Color(0xFF002F66)
val GpsPillContainer = Color(0xFFDCE5E0)
val GpsPillOn = Color(0xFF1F2E2A)
val NonePillContainer = Color(0xFFFFD9D2)
val NonePillOn = Color(0xFF6C1C10)

// Tonal containers (dark) — dark-mode pills use dimmer containers
val RtkPillContainerDark = Color(0xFF00493D)
val RtkPillOnDark = Color(0xFFA5F2D9)
val FloatPillContainerDark = Color(0xFF4A3F00)
val FloatPillOnDark = Color(0xFFFDF0B3)
val DgpsPillContainerDark = Color(0xFF002F66)
val DgpsPillOnDark = Color(0xFFD6E3FF)
val GpsPillContainerDark = Color(0xFF1F2E2A)
val GpsPillOnDark = Color(0xFFDCE5E0)
val NonePillContainerDark = Color(0xFF6C1C10)
val NonePillOnDark = Color(0xFFFFD9D2)

// Legacy success/warning aliases (kept for compatibility with existing code)
val SuccessContainer = RtkPillContainer
val OnSuccessContainer = RtkPillOn
val WarningContainer = FloatPillContainer
val OnWarningContainer = FloatPillOn

// ── Accuracy σH bands (light) ──
val AccuracyGood = FixRtkGreen            // σH < 2 cm
val AccuracyOk = FixFloatOrange           // σH < 5 cm
val AccuracyPoor = FixNoneRed             // σH ≥ 5 cm
val AccuracyGoodDark = FixRtkGreenDark
val AccuracyOkDark = FixFloatOrangeDark
val AccuracyPoorDark = FixNoneRedDark

// ── Correction age bands ──
val CorrectionFresh = FixRtkGreen
val CorrectionStale = FixFloatOrange
val CorrectionDead = FixNoneRed
val CorrectionFreshDark = FixRtkGreenDark
val CorrectionStaleDark = FixFloatOrangeDark
val CorrectionDeadDark = FixNoneRedDark

// ── Stakeout ΔE/ΔN distance bands ──
val StakeoutFar = FixNoneRed
val StakeoutClose = FixFloatOrange
val StakeoutOnPoint = FixRtkGreen
val StakeoutFarDark = FixNoneRedDark
val StakeoutCloseDark = FixFloatOrangeDark
val StakeoutOnPointDark = FixRtkGreenDark

// ── Recording ──
val RecordingActive = Color(0xFFD1416A)
val RecordingProgress = md_theme_light_primary
val RecordingActiveDark = Color(0xFFFFD9E0)
val RecordingProgressDark = md_theme_dark_primary

// ─────────────────────────────────────────────────────────────────────────────
//  Constellation colors — matching v2 design so map/skyplot/chips align.
// ─────────────────────────────────────────────────────────────────────────────
object ConstellationColors {
    val gps = Color(0xFF4A73C4)           // USA — blue
    val gpsContainer = Color(0xFFD6E3FF)
    val glonass = Color(0xFF9B3A2E)       // RU — red
    val glonassContainer = Color(0xFFFFD9D2)
    val galileo = Color(0xFF1C6E5A)       // EU — teal (primary family)
    val galileoContainer = Color(0xFFA5F2D9)
    val beidou = Color(0xFFB0522C)        // CN — ochre (tertiary family)
    val beidouContainer = Color(0xFFFFDBC9)
}
