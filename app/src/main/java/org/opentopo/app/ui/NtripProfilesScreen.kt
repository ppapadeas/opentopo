package org.opentopo.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.opentopo.app.ntrip.NtripConnectionState
import org.opentopo.app.ntrip.NtripProfile
import org.opentopo.app.ui.theme.CoordinateFont
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen profiles management view.
 *
 * Hero card for the currently-active profile (with live status strip +
 * host/mountpoint/user/bitrate grid), a saved-list with swipe-to-delete and
 * long-press menu, and a dashed "New profile" tile. Designed to be pushed
 * as a destination above MainMapScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NtripProfilesScreen(
    profiles: List<NtripProfile>,
    activeProfile: NtripProfile?,
    state: NtripConnectionState,
    onBack: () -> Unit,
    onActivate: (NtripProfile) -> Unit,
    onEdit: (NtripProfile) -> Unit,
    onDuplicate: (NtripProfile) -> Unit,
    onDelete: (NtripProfile) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            // Reserve safe space for system status bar (time, signal, battery)
            // AND the gesture-nav handle at the bottom. Uses the system-bars
            // insets so it respects camera cutouts and rotated layouts too.
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        TopRow(onBack = onBack, onAdd = onAdd)

        Spacer(Modifier.height(10.dp))
        Text(
            text = "One profile is active at a time. Swipe a row to delete, long-press to duplicate.",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (activeProfile != null) {
            Spacer(Modifier.height(14.dp))
            HeroCard(
                profile = activeProfile,
                state = state,
                onEdit = { onEdit(activeProfile) },
            )
        }

        val saved = profiles.filter { it.id != activeProfile?.id }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "SAVED · ${saved.size}",
            modifier = Modifier.padding(start = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.W700,
            letterSpacing = 0.1.em,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            saved.forEach { profile ->
                SavedProfileRow(
                    profile = profile,
                    onActivate = { onActivate(profile) },
                    onEdit = { onEdit(profile) },
                    onDuplicate = { onDuplicate(profile) },
                    onDelete = { onDelete(profile) },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        NewProfileTile(onClick = onAdd)

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────── top row ───────────────────────────

@Composable
private fun TopRow(onBack: () -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "GNSS · NTRIP",
                fontFamily = CoordinateFont,
                fontSize = 11.sp,
                letterSpacing = 0.06.em,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Profiles",
                fontSize = 24.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.01).em,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Add profile",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

// ─────────────────────────── hero card ───────────────────────────

@Composable
private fun HeroCard(
    profile: NtripProfile,
    state: NtripConnectionState,
    onEdit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1C6E5A)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = profile.code.take(2).uppercase(),
                        color = Color.White,
                        fontFamily = CoordinateFont,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W700,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    // Status strip
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.W700,
                            letterSpacing = 0.1.em,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Text(
                            text = statusDetail(state),
                            fontFamily = CoordinateFont,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = profile.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W700,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                // Trailing edit button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1F00493D))
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit profile",
                        tint = Color(0xFF00261C),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // KV grid — 2 columns of 2 pairs
            val bitrateValue = when (state) {
                is NtripConnectionState.Live -> "${state.bitrate} B/s"
                else -> "—"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KvPair("HOST", "${profile.host}:${profile.port}")
                    KvPair("USER", profile.username.ifBlank { "—" })
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KvPair("MOUNTPOINT", profile.mountpoint.ifBlank { "—" })
                    KvPair("BITRATE", bitrateValue)
                }
            }
        }
    }
}

@Composable
private fun KvPair(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.W500,
            letterSpacing = 0.08.em,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontFamily = CoordinateFont,
            fontSize = 11.sp,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private fun statusDetail(state: NtripConnectionState): String = when (state) {
    is NtripConnectionState.Live -> "live · ${"%.1f".format(state.ageSeconds)} s"
    NtripConnectionState.Connecting -> "connecting…"
    is NtripConnectionState.Stale -> "stale · ${"%.0f".format(state.ageSeconds)} s"
    NtripConnectionState.Disconnected -> "idle"
    is NtripConnectionState.Error -> "error"
    NtripConnectionState.Empty -> "idle"
}

// ─────────────────────────── saved list row ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SavedProfileRow(
    profile: NtripProfile,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            if (target == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        SavedProfileCardContent(
            profile = profile,
            onActivate = onActivate,
            onEdit = onEdit,
            onDuplicate = onDuplicate,
            onDelete = onDelete,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedProfileCardContent(
    profile: NtripProfile,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { /* no-op on card body; use Activate button */ },
                        onLongClick = { menuExpanded = true },
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(profile.tintColor)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = profile.code.take(2).uppercase(),
                        color = Color(profile.badgeFgColor),
                        fontFamily = CoordinateFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W700,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W600,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${profile.host}:${profile.port} · " +
                            profile.mountpoint.ifBlank { "no mountpoint" },
                        fontFamily = CoordinateFont,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatLastUsed(profile.lastUsedAt),
                        fontFamily = CoordinateFont,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                OutlinedButton(
                    onClick = onActivate,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 7.dp,
                    ),
                ) {
                    Text(
                        text = "Activate",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W700,
                    )
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Duplicate") },
                    onClick = {
                        menuExpanded = false
                        onDuplicate()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        }
    }
}

private fun formatLastUsed(lastUsedAt: Long): String {
    if (lastUsedAt == 0L) return "saved · never used"
    val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
    return "saved · last used ${fmt.format(Date(lastUsedAt))}"
}

// ─────────────────────────── new profile tile ───────────────────────────

@Composable
private fun NewProfileTile(onClick: () -> Unit) {
    val dashColor = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
            .drawBehind {
                val stroke = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                )
                val r = 18.dp.toPx()
                drawRoundRect(
                    color = dashColor,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(r, r),
                    style = stroke,
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "New profile",
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Scan sourcetable · import .ntrip · manual",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
