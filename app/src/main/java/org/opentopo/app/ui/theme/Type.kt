package org.opentopo.app.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Monospace font for all coordinate and numeric displays. */
val CoordinateFont = FontFamily.Monospace

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val OpenTopoTypography = Typography(
    displayLarge = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.W300, letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.W300, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W700, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W700, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W700, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W700, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W700, letterSpacing = 0.8.sp),
    // M3 Expressive emphasized variants
    displayLargeEmphasized = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp),
    displayMediumEmphasized = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    displaySmallEmphasized = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    headlineLargeEmphasized = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
    headlineMediumEmphasized = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
    headlineSmallEmphasized = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
    titleLargeEmphasized = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
    titleMediumEmphasized = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.sp),
    titleSmallEmphasized = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.sp),
    bodyLargeEmphasized = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W600, letterSpacing = 0.sp),
    bodyMediumEmphasized = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W600, letterSpacing = 0.sp),
    bodySmallEmphasized = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W600, letterSpacing = 0.sp),
    labelLargeEmphasized = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.2.sp),
    labelMediumEmphasized = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.3.sp),
    labelSmallEmphasized = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp),
)
