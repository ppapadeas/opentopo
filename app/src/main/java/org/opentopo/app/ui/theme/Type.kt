package org.opentopo.app.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.opentopo.app.R

// ─────────────────────────────────────────────────────────────────────────────
//  OpenTopo v2.0 typography — Material 3 Expressive scales.
//
//  Roboto Flex for UI (variable opsz + wght, scales cleanly from 11 sp labels
//  to 57 sp displays). JetBrains Mono for every coordinate, delta, and σ value
//  so surveyors can line up decimals at a glance. Every scale has an
//  *Emphasized variant at +200 weight for active/selected states.
// ─────────────────────────────────────────────────────────────────────────────

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val robotoFlex = GoogleFont("Roboto Flex")
private val jetbrainsMono = GoogleFont("JetBrains Mono")

private val RobotoFlex = FontFamily(
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.W400),
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.W500),
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.W600),
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.W700),
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.W800),
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.W300),
    Font(googleFont = robotoFlex, fontProvider = googleFontProvider, weight = FontWeight.W400, style = FontStyle.Italic),
)

private val JetBrainsMono = FontFamily(
    Font(googleFont = jetbrainsMono, fontProvider = googleFontProvider, weight = FontWeight.W400),
    Font(googleFont = jetbrainsMono, fontProvider = googleFontProvider, weight = FontWeight.W500),
    Font(googleFont = jetbrainsMono, fontProvider = googleFontProvider, weight = FontWeight.W700),
)

/**
 * Monospace font family for all coordinate and numeric displays.
 * Resolves to JetBrains Mono (downloaded via Google Fonts provider) with
 * `FontFamily.Monospace` as a no-network fallback.
 */
val CoordinateFont: FontFamily = JetBrainsMono

/**
 * Survey-specific text styles that don't live on [Typography]. Usage:
 * ```
 * Text(..., style = MonoCoord)   // 20sp, weight 500 — EGSA87 E/N/H
 * Text(..., style = MonoDelta)   // 15sp, weight 400 — ΔE/ΔN/ΔH, σ values
 * ```
 */
val MonoCoord = TextStyle(
    fontFamily = JetBrainsMono,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    fontWeight = FontWeight.W500,
    letterSpacing = 0.sp,
)

val MonoDelta = TextStyle(
    fontFamily = JetBrainsMono,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.W400,
    letterSpacing = 0.sp,
)

/** Compact overline style used above TonalCard blocks — uppercase + letter-spaced. */
val LabelOverline = TextStyle(
    fontFamily = JetBrainsMono,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.W500,
    letterSpacing = 1.1.sp,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val OpenTopoTypography = Typography(
    // Displays — driven by variable opsz 144
    displayLarge = TextStyle(fontFamily = RobotoFlex, fontSize = 57.sp, lineHeight = 64.sp, fontWeight = FontWeight.W500, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = RobotoFlex, fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.W500, letterSpacing = (-0.15).sp),
    displaySmall = TextStyle(fontFamily = RobotoFlex, fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp),
    // Headlines
    headlineLarge = TextStyle(fontFamily = RobotoFlex, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = RobotoFlex, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = RobotoFlex, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp),
    // Titles
    titleLarge = TextStyle(fontFamily = RobotoFlex, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = RobotoFlex, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.W500, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = RobotoFlex, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.W500, letterSpacing = 0.1.sp),
    // Body
    bodyLarge = TextStyle(fontFamily = RobotoFlex, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.W400, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = RobotoFlex, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.W400, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = RobotoFlex, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.W400, letterSpacing = 0.4.sp),
    // Labels
    labelLarge = TextStyle(fontFamily = RobotoFlex, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.W600, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = RobotoFlex, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.W600, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = RobotoFlex, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.W600, letterSpacing = 0.5.sp),
    // M3E Emphasized variants — +200 wght for active/selected emphasis
    displayLargeEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 57.sp, lineHeight = 64.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.25).sp),
    displayMediumEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.15).sp),
    displaySmallEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp),
    headlineLargeEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp),
    headlineMediumEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp),
    headlineSmallEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp),
    titleLargeEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp),
    titleMediumEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.W700, letterSpacing = 0.15.sp),
    titleSmallEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.W700, letterSpacing = 0.1.sp),
    bodyLargeEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.W600, letterSpacing = 0.5.sp),
    bodyMediumEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.W600, letterSpacing = 0.25.sp),
    bodySmallEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.W600, letterSpacing = 0.4.sp),
    labelLargeEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.W800, letterSpacing = 0.2.sp),
    labelMediumEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.W800, letterSpacing = 0.5.sp),
    labelSmallEmphasized = TextStyle(fontFamily = RobotoFlex, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.W800, letterSpacing = 0.5.sp),
)

private fun TextStyle.plusSp(extra: TextUnit): TextStyle =
    copy(fontSize = (fontSize.value + extra.value).sp)

/** Typography with +4sp on all text styles for glove mode field use. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val GloveTypography: Typography by lazy {
    val bump = 4.sp
    Typography(
        displayLarge = OpenTopoTypography.displayLarge.plusSp(bump),
        displayMedium = OpenTopoTypography.displayMedium.plusSp(bump),
        displaySmall = OpenTopoTypography.displaySmall.plusSp(bump),
        headlineLarge = OpenTopoTypography.headlineLarge.plusSp(bump),
        headlineMedium = OpenTopoTypography.headlineMedium.plusSp(bump),
        headlineSmall = OpenTopoTypography.headlineSmall.plusSp(bump),
        titleLarge = OpenTopoTypography.titleLarge.plusSp(bump),
        titleMedium = OpenTopoTypography.titleMedium.plusSp(bump),
        titleSmall = OpenTopoTypography.titleSmall.plusSp(bump),
        bodyLarge = OpenTopoTypography.bodyLarge.plusSp(bump),
        bodyMedium = OpenTopoTypography.bodyMedium.plusSp(bump),
        bodySmall = OpenTopoTypography.bodySmall.plusSp(bump),
        labelLarge = OpenTopoTypography.labelLarge.plusSp(bump),
        labelMedium = OpenTopoTypography.labelMedium.plusSp(bump),
        labelSmall = OpenTopoTypography.labelSmall.plusSp(bump),
        displayLargeEmphasized = OpenTopoTypography.displayLargeEmphasized.plusSp(bump),
        displayMediumEmphasized = OpenTopoTypography.displayMediumEmphasized.plusSp(bump),
        displaySmallEmphasized = OpenTopoTypography.displaySmallEmphasized.plusSp(bump),
        headlineLargeEmphasized = OpenTopoTypography.headlineLargeEmphasized.plusSp(bump),
        headlineMediumEmphasized = OpenTopoTypography.headlineMediumEmphasized.plusSp(bump),
        headlineSmallEmphasized = OpenTopoTypography.headlineSmallEmphasized.plusSp(bump),
        titleLargeEmphasized = OpenTopoTypography.titleLargeEmphasized.plusSp(bump),
        titleMediumEmphasized = OpenTopoTypography.titleMediumEmphasized.plusSp(bump),
        titleSmallEmphasized = OpenTopoTypography.titleSmallEmphasized.plusSp(bump),
        bodyLargeEmphasized = OpenTopoTypography.bodyLargeEmphasized.plusSp(bump),
        bodyMediumEmphasized = OpenTopoTypography.bodyMediumEmphasized.plusSp(bump),
        bodySmallEmphasized = OpenTopoTypography.bodySmallEmphasized.plusSp(bump),
        labelLargeEmphasized = OpenTopoTypography.labelLargeEmphasized.plusSp(bump),
        labelMediumEmphasized = OpenTopoTypography.labelMediumEmphasized.plusSp(bump),
        labelSmallEmphasized = OpenTopoTypography.labelSmallEmphasized.plusSp(bump),
    )
}
