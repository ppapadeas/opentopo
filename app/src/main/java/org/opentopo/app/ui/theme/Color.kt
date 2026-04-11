package org.opentopo.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── App primary colors ──
val PrimaryLight = Color(0xFF2E7D32)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFA5D6A7)
val OnPrimaryContainerLight = Color(0xFF002204)

val PrimaryDark = Color(0xFF81C784)
val OnPrimaryDark = Color(0xFF003909)
val PrimaryContainerDark = Color(0xFF1B5E20)
val OnPrimaryContainerDark = Color(0xFFA5D6A7)

// ── Fix quality colors ──
val FixRtkGreen = Color(0xFF2E7D32)
val FixFloatOrange = Color(0xFFEF6C00)
val FixDgpsYellow = Color(0xFFF9A825)
val FixGpsBlue = Color(0xFF1565C0)
val FixNoneRed = Color(0xFFC62828)
val OnFixColor = Color(0xFFFFFFFF)

// ── Accuracy thresholds ──
val AccuracyGood = Color(0xFF2E7D32)      // < 0.02m
val AccuracyOk = Color(0xFFF9A825)        // 0.02 - 0.05m
val AccuracyPoor = Color(0xFFC62828)      // > 0.05m

// ── Correction age thresholds ──
val CorrectionFresh = Color(0xFF2E7D32)   // < 2s
val CorrectionStale = Color(0xFFF9A825)   // 2-5s
val CorrectionDead = Color(0xFFC62828)    // > 5s

// ── Stakeout proximity ──
val StakeoutFar = Color(0xFFC62828)       // > 1.0m
val StakeoutClose = Color(0xFFF9A825)     // 0.1 - 1.0m
val StakeoutOnPoint = Color(0xFF2E7D32)   // < 0.1m

// ── Recording ──
val RecordingActive = Color(0xFFC62828)
val RecordingProgress = Color(0xFF2E7D32)
