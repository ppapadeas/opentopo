package org.opentopo.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.opentopo.app.db.AppDatabase
import org.opentopo.app.survey.SurveyManager
import org.opentopo.app.ui.theme.CoordinateFont
import org.opentopo.transform.HeposTransform

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun ToolsPanel(
    db: AppDatabase,
    surveyManager: SurveyManager?,
    transform: HeposTransform?,
    modifier: Modifier = Modifier,
    onOpenCoordConverter: () -> Unit = {},
    onOpenGysSearch: () -> Unit = {},
    onOpenImport: () -> Unit = {},
    onOpenExportProject: () -> Unit = {},
    onOpenAreaPerimeter: () -> Unit = {},
    onOpenTransformPipeline: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenRecentActivity: () -> Unit = {},
    onOpenWhatsNew: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
) {
    @Suppress("UNUSED_VARIABLE")
    var query by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        // ── 1. Page header (margin-top 20 dp) ──
        Spacer(Modifier.height(20.dp))
        Text(
            text = "TOOLS \u00B7 SETTINGS \u00B7 ABOUT",
            fontFamily = CoordinateFont,
            fontSize = 11.sp,
            letterSpacing = 0.06.em,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "More",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.02).em,
            lineHeight = 30.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // ── 2. Search bar (margin-top 14 dp) ──
        Spacer(Modifier.height(14.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Search tools & settings",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── 3. Tools section (margin-top 18 dp) ──
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "TOOLS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.em,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "6",
                fontFamily = CoordinateFont,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))

        val tiles = listOf(
            ToolTile("CC", "Coord. converter", "EGSA87 \u2194 WGS84 \u2194 UTM",
                Color(0xFFA5F2D9), Color(0xFF00493D), onOpenCoordConverter),
            ToolTile("GY", "GYS search", "25,259 trig points",
                Color(0xFFD6E3FF), Color(0xFF002F66), onOpenGysSearch),
            ToolTile("IM", "Import", "CSV \u00B7 DXF \u00B7 GeoJSON",
                Color(0xFFFFDBC9), Color(0xFF3A1100), onOpenImport),
            ToolTile("EX", "Export project", "LandXML \u00B7 SHP \u00B7 PDF",
                Color(0xFFFDF0B3), Color(0xFF4A3F00), onOpenExportProject),
            ToolTile("AR", "Area & perimeter", "from current polygon",
                Color(0xFFDCE5E0), Color(0xFF1F2E2A), onOpenAreaPerimeter),
            ToolTile("TR", "Transform pipeline", "HEPOS \u00B7 custom 7-param",
                Color(0xFFFFD9D2), Color(0xFF6C1C10), onOpenTransformPipeline),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (rowIdx in 0 until 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ToolTileView(tile = tiles[rowIdx * 2], modifier = Modifier.weight(1f))
                    ToolTileView(tile = tiles[rowIdx * 2 + 1], modifier = Modifier.weight(1f))
                }
            }
        }

        // ── 4. App list (margin-top 18 dp) ──
        Spacer(Modifier.height(18.dp))
        Text(
            text = "APP",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.em,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(10.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                AppListRow(
                    icon = Icons.Outlined.Settings,
                    title = "Settings",
                    sub = "Units, coordinates, display, storage",
                    onClick = onOpenSettings,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                )
                AppListRow(
                    icon = Icons.Outlined.Schedule,
                    title = "Recent activity",
                    sub = "Last 30 days \u00B7 142 points recorded",
                    onClick = onOpenRecentActivity,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                )
                AppListRow(
                    icon = Icons.Outlined.StarOutline,
                    title = "What's new in v2.0",
                    sub = "M3 Expressive \u00B7 NTRIP profiles \u00B7 AMOLED",
                    onClick = onOpenWhatsNew,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                )
                AppListRow(
                    icon = Icons.Outlined.Info,
                    title = "About OpenTopo",
                    sub = "v2.0.0 \u00B7 AGPLv3 \u00B7 opentopo.gr",
                    onClick = onOpenAbout,
                )
            }
        }

        // ── 5. Trailing spacer ──
        Spacer(Modifier.height(24.dp))
    }
}

private data class ToolTile(
    val code: String,
    val title: String,
    val sub: String,
    val tint: Color,
    val fg: Color,
    val onClick: () -> Unit,
)

@Composable
private fun ToolTileView(
    tile: ToolTile,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 100.dp)
            .clickable { tile.onClick() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Code chip 38x38
            Surface(
                color = tile.tint,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = tile.code,
                        fontFamily = CoordinateFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = tile.fg,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = tile.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.005).em,
                    lineHeight = 13.sp * 1.2f,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = tile.sub,
                    fontSize = 11.sp,
                    lineHeight = 11.sp * 1.35f,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AppListRow(
    icon: ImageVector,
    title: String,
    sub: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.005).em,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = sub,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}
