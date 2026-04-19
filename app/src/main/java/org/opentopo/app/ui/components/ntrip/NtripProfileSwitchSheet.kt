package org.opentopo.app.ui.components.ntrip

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.opentopo.app.ntrip.NtripProfile
import org.opentopo.app.ui.theme.CoordinateFont

/**
 * Compact bottom-sheet profile switcher.
 *
 * Shows one row per saved NtripProfile with a colored two-letter badge.
 * The row matching [activeProfileId] is highlighted with primaryContainer
 * background and a trailing primary dot. Tapping a non-active row activates
 * it and dismisses the sheet; tapping "Manage profiles…" opens the full
 * management screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NtripProfileSwitchSheet(
    profiles: List<NtripProfile>,
    activeProfileId: String?,
    onSelect: (NtripProfile) -> Unit,
    onManageClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        // scrim defaults to MaterialTheme scrim at 32% — M3 default; spec's
        // 45% black is close enough that we accept the default to keep the
        // sheet idiomatic. If required we can override with a custom scrim.
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
        ) {
            Text(
                text = "SWITCH PROFILE",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = 0.08.em,
            )
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                profiles.forEach { profile ->
                    val isActive = profile.id == activeProfileId
                    ProfileRow(
                        profile = profile,
                        isActive = isActive,
                        onClick = {
                            if (!isActive) {
                                onSelect(profile)
                                onDismiss()
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            ManageFooterButton(onClick = onManageClick)
        }
    }
}

@Composable
private fun ProfileRow(
    profile: NtripProfile,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading badge 22x22, mono 9sp weight 700
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(profile.tintColor)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = profile.code.take(2).uppercase(),
                color = Color(profile.badgeFgColor),
                fontFamily = CoordinateFont,
                fontSize = 9.sp,
                fontWeight = FontWeight.W700,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = profile.displayName,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.W600 else FontWeight.W400,
        )
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun ManageFooterButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Manage profiles…",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.W700,
            )
        }
    }
}
