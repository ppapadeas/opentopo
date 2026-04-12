package org.opentopo.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Monospace font for all coordinate and numeric displays. */
val CoordinateFont = FontFamily.Monospace

val OpenTopoTypography = Typography(
    displayLarge = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.W300, letterSpacing = (-2).sp),
    displayMedium = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.W300, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.W600, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W600, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W600, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W600, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W600, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W600, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp),
    bodySmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W600, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W600, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.W600, letterSpacing = 0.8.sp),
)
