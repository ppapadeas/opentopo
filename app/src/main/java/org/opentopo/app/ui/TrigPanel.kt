package org.opentopo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.opentopo.app.survey.TrigPoint
import org.opentopo.app.ui.components.FixStatusPill
import org.opentopo.app.ui.theme.CoordinateFont

/**
 * v2 Trig panel.
 *
 * Behaves contextually:
 *  - When [selectedTrigPoint] is null → scroll list of cached trig points.
 *  - When [selectedTrigPoint] is non-null → "Trig Point Verify" screen matching the
 *    v2 mockup (TrigScreen, lines 1332–1410 of `opentopo-v2.html`).
 *
 * The verify screen shows published EGSA87 coordinates, measured RTK coordinates,
 * and residuals. It exposes Stakeout / Submit actions. The actual verify *action*
 * (running HeposTransform, computing residuals) still lives in `MainMapScreen`
 * behind the map tap → AlertDialog flow. This panel is purely presentational.
 */
@Composable
internal fun TrigPanel(
    trigPoints: List<TrigPoint>,
    onSelect: (TrigPoint) -> Unit,
    selectedTrigPoint: TrigPoint? = null,
    verificationResult: VerificationResult? = null,
    toleranceM: Double = 0.02,
    onBack: () -> Unit = {},
    onStakeout: (TrigPoint) -> Unit = {},
    onSubmit: (VerificationResult) -> Unit = {},
) {
    if (selectedTrigPoint != null) {
        TrigVerifyScreen(
            trigPoint = selectedTrigPoint,
            verificationResult = verificationResult,
            toleranceM = toleranceM,
            onBack = onBack,
            onStakeout = { onStakeout(selectedTrigPoint) },
            onSubmit = { verificationResult?.let(onSubmit) },
        )
        return
    }

    if (trigPoints.isEmpty()) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Pan the map to load nearby GYS trig points.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val sorted = remember(trigPoints) {
        trigPoints.sortedWith(
            compareBy(nullsLast<Double>()) { it.distanceM },
        )
    }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        sorted.forEach { tp ->
            TrigListItem(tp, onSelect)
        }
    }
}

@Composable
private fun TrigListItem(tp: TrigPoint, onSelect: (TrigPoint) -> Unit) {
    Surface(
        onClick = { onSelect(tp) },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.PinDrop,
                contentDescription = null,
                tint = when (tp.status) {
                    "OK" -> Color(0xFF4CAF50)
                    "DAMAGED" -> Color(0xFFFF9800)
                    "DESTROYED" -> Color(0xFFF44336)
                    "MISSING" -> Color(0xFF9C27B0)
                    else -> Color(0xFF9E9E9E)
                },
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "GYS ${tp.gysId}",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = CoordinateFont,
                    fontWeight = FontWeight.Bold,
                )
                tp.name?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            tp.distanceM?.let {
                Text(
                    "${"%.0f".format(it)} m",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = CoordinateFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/* ─────────────────────────── Verify screen ─────────────────────────── */

/** Mono text style used across the verify screen for EGSA87 coordinate rows. */
private val CoordMono16 = TextStyle(
    fontFamily = CoordinateFont,
    fontSize = 16.sp,
    lineHeight = 23.sp, // 1.45 × 16sp
    fontWeight = FontWeight.W500,
)

private val OverlineSmall = TextStyle(
    fontFamily = CoordinateFont,
    fontSize = 10.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.W700,
    letterSpacing = 1.0.sp, // ≈ 0.1em at 10sp
)

private val ChipMono11 = TextStyle(
    fontFamily = CoordinateFont,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.66.sp, // ≈ 0.06em at 11sp
)

@Composable
private fun TrigVerifyScreen(
    trigPoint: TrigPoint,
    verificationResult: VerificationResult?,
    toleranceM: Double,
    onBack: () -> Unit,
    onStakeout: () -> Unit,
    onSubmit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
    ) {
        Spacer(Modifier.height(18.dp))

        // 1 — Header row: back button + title block
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = colors.surfaceContainer,
                    contentColor = colors.onSurface,
                ),
                shape = CircleShape,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "GYS TRIG POINT \u00B7 vathra.xyz",
                    style = ChipMono11,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    "GYS.${trigPoint.gysId}${trigPoint.name?.let { " \u2014 $it" } ?: ""}",
                    fontSize = 22.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.W700,
                    letterSpacing = (-0.22).sp, // ≈ -0.01em at 22sp
                    color = colors.onSurface,
                )
            }
        }

        // 2 — Status chip row
        Spacer(Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // RTK-green-container tinted pill — use FixStatusPill with RTK quality (4)
            // and override the label via a Surface wrapper for the "OK · Verified YYYY" copy.
            StatusVerifiedPill(statusText = trigPoint.status?.let { "$it \u00B7 Verified 2024" } ?: "Verified 2024")
            Text(
                pillarOrderLabel(trigPoint),
                style = ChipMono11,
                color = colors.onSurfaceVariant,
            )
        }

        // 3 — Published card
        Spacer(Modifier.height(14.dp))
        CoordCardSurface {
            Text(
                "PUBLISHED \u00B7 GYS",
                style = OverlineSmall,
                color = colors.primary,
            )
            Spacer(Modifier.height(6.dp))
            CoordTriple(
                e = formatMeters(trigPoint.egsa87Easting),
                n = formatMeters(trigPoint.egsa87Northing),
                h = trigPoint.elevation?.let { formatMeters(it) + " m" },
                muteColor = colors.onSurfaceVariant,
                primaryColor = colors.onSurface,
            )
        }

        // 4 — Measured card
        Spacer(Modifier.height(10.dp))
        CoordCardSurface {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "MEASURED \u00B7 ${measuredHeader(verificationResult)}",
                    style = OverlineSmall,
                    color = colors.primary,
                )
                if (verificationResult != null) {
                    CompactFixPill(
                        fixQuality = verificationResult.fixQuality,
                        sigmaCm = verificationResult.horizontalAccuracy,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            CoordTriple(
                e = formatMeters(verificationResult?.measuredE),
                n = formatMeters(verificationResult?.measuredN),
                h = verificationResult?.measuredH?.let { formatMeters(it) + " m" },
                muteColor = colors.onSurfaceVariant,
                primaryColor = colors.onSurface,
            )
        }

        // 5 — Residuals hero card
        Spacer(Modifier.height(10.dp))
        ResidualsHeroCard(
            verificationResult = verificationResult,
            toleranceM = toleranceM,
        )

        // 6 — Gap between residuals card and action row. Inside a verticalScroll
        //     column a fill-remaining spacer has no meaning, so we use a fixed gap.
        Spacer(Modifier.height(16.dp))

        // 7 — Bottom actions
        val passing = verificationResult?.let { it.horizontalResidual <= toleranceM } == true
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onStakeout,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text("Stakeout", fontSize = 14.sp)
            }
            Button(
                onClick = onSubmit,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                enabled = passing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) {
                Text("Submit to vathra.xyz", fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

/* ───────── Verify-screen helpers ───────── */

/** surfaceContainerLow rounded 20dp card with the mockup's 14×16dp padding. */
@Composable
private fun CoordCardSurface(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            content = content,
        )
    }
}

@Composable
private fun CoordTriple(
    e: String,
    n: String,
    h: String?,
    muteColor: Color,
    primaryColor: Color,
) {
    Text("E  $e", style = CoordMono16, color = primaryColor)
    Text("N  $n", style = CoordMono16, color = primaryColor)
    if (!h.isNullOrBlank()) {
        Text("H     $h", style = CoordMono16, color = muteColor)
    }
}

/**
 * Small status pill styled like `.pill.rtk` in the mockup — green container, dot,
 * compact text. Uses [FixStatusPill] tokens for consistency.
 */
@Composable
private fun StatusVerifiedPill(statusText: String) {
    // FixStatusPill with RTK quality = 4 gives us the RTK container/content tokens.
    // We override the label via `extras` style trick — but FixStatusPill embeds its
    // own label, so we replicate its visual with a Surface here to control copy.
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(percent = 50),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .padding(0.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(8.dp),
                ) {}
            }
            Text(
                statusText,
                fontFamily = CoordinateFont,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** Right-side measured pill — `0.9 cm`, compact padding. */
@Composable
private fun CompactFixPill(fixQuality: Int, sigmaCm: Double?) {
    // Use the shared FixStatusPill to stay aligned with the fix-color ramp, but
    // fall back to a hand-rolled mini chip when sigmaCm is non-null and we need
    // the short "0.9 cm" copy the mockup shows.
    if (fixQuality == 4 && sigmaCm != null) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(percent = 50),
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(6.dp),
                ) {}
                Text(
                    formatCm(sigmaCm),
                    fontFamily = CoordinateFont,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    } else {
        FixStatusPill(
            fixQuality = fixQuality,
            extras = sigmaCm?.let { formatCm(it) },
        )
    }
}

@Composable
private fun ResidualsHeroCard(
    verificationResult: VerificationResult?,
    toleranceM: Double,
) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer // #A5F2D9 in light
    val onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer // #00261C in light
    val errorBg = MaterialTheme.colorScheme.errorContainer
    val errorOn = MaterialTheme.colorScheme.onErrorContainer

    val passing = verificationResult?.let { it.horizontalResidual <= toleranceM } == true
    val bg = if (verificationResult != null && !passing) errorBg else containerColor
    val fg = if (verificationResult != null && !passing) errorOn else onContainerColor

    Surface(
        color = bg,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                "RESIDUALS",
                style = OverlineSmall,
                color = fg.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(8.dp))

            val cells: List<Pair<String, String?>> = listOf(
                "\u0394E" to verificationResult?.deltaE?.let { signedMeters(it) },
                "\u0394N" to verificationResult?.deltaN?.let { signedMeters(it) },
                "\u0394H" to verificationResult?.deltaH?.let { signedMeters(it) },
                "\u2016H\u2016" to verificationResult?.horizontalResidual?.let { "%.3f".format(it) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                cells.forEach { (label, value) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            label,
                            fontFamily = CoordinateFont,
                            fontSize = 10.sp,
                            color = fg.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            value ?: "\u2014",
                            fontFamily = CoordinateFont,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W700,
                            color = fg,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "m",
                            fontFamily = CoordinateFont,
                            fontSize = 9.sp,
                            color = fg.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            val footer = when {
                verificationResult == null -> "awaiting measurement"
                passing -> "within ${"%.0f".format(toleranceM * 100)} cm tolerance \u2014 safe to submit"
                else -> "exceeds tolerance \u2014 re-measure"
            }
            Text(
                footer,
                fontFamily = CoordinateFont,
                fontSize = 11.sp,
                color = fg.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/* ───────── Formatters ───────── */

private fun formatMeters(value: Double?): String =
    if (value == null) "\u2014" else "%,.3f".format(value).replace(',', '\u2009')

private fun signedMeters(value: Double): String {
    val sign = when {
        value > 0.0 -> "+"
        value < 0.0 -> "\u2212" // minus sign U+2212 matches the mockup
        else -> ""
    }
    return sign + "%.3f".format(kotlin.math.abs(value))
}

private fun formatCm(sigmaM: Double): String = "%.1f cm".format(sigmaM * 100.0)

private fun pillarOrderLabel(tp: TrigPoint): String {
    val orderLabel = when (tp.pointOrder) {
        1 -> "1st order"
        2 -> "2nd order"
        3 -> "3rd order"
        4 -> "4th order"
        else -> "unknown order"
    }
    return "pillar \u00B7 $orderLabel"
}

private fun measuredHeader(result: VerificationResult?): String {
    if (result == null) return "awaiting fix"
    return when (result.fixQuality) {
        4 -> "RTK 10 s"
        5 -> "RTK float"
        2 -> "DGPS"
        1 -> "GPS"
        else -> "No fix"
    }
}
