package org.opentopo.app.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opentopo.app.db.AppDatabase
import org.opentopo.app.db.PointEntity
import org.opentopo.app.db.ProjectEntity
import org.opentopo.app.export.CsvExporter
import org.opentopo.app.export.CsvImporter
import org.opentopo.app.export.DxfExporter
import org.opentopo.app.export.GeoJsonExporter
import org.opentopo.app.export.ShapefileExporter
import org.opentopo.app.survey.SurveyManager
import org.opentopo.app.ui.theme.CoordinateFont
import org.opentopo.transform.HeposTransform
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToolsPanel(
    db: AppDatabase,
    surveyManager: SurveyManager?,
    transform: HeposTransform?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        // ── EXPORT / IMPORT ──
        Text(
            "EXPORT / IMPORT",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
        ) {
            ExportImportSection(db = db, surveyManager = surveyManager)
        }

        // ── COORDINATE TOOLS ──
        Text(
            "COORDINATE TOOLS",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
        TransformPanel(transform = transform)

        // ── SETTINGS ──
        Text(
            "SETTINGS",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
        SettingsPanel()

        Spacer(Modifier.height(16.dp))
    }
}

// ── Export/Import section (uses active project from SurveyManager) ──

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExportImportSection(
    db: AppDatabase,
    surveyManager: SurveyManager?,
) {
    val activeProjectId by surveyManager?.activeProjectId?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val projects by db.projectDao().getAll().collectAsState(initial = emptyList())
    val activeProject = projects.find { it.id == activeProjectId }

    var exportStatus by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val proj = activeProject
        if (proj == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Select a project from the header to export data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Text(
                "Project: ${proj.name}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            /* ---- Import CSV ---- */
            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent(),
            ) { uri ->
                uri?.let {
                    scope.launch {
                        try {
                            val input = context.contentResolver.openInputStream(it)
                                ?: return@launch
                            val points = CsvImporter.import(input, proj.id)
                            input.close()
                            withContext(Dispatchers.IO) {
                                points.forEach { pt -> db.pointDao().insert(pt) }
                            }
                            exportStatus = "Imported ${points.size} points"
                        } catch (e: Exception) {
                            exportStatus = "Import failed: ${e.message}"
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { importLauncher.launch("text/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Icon(
                    Icons.Outlined.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Import CSV")
            }

            Text(
                "Export format",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isExporting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ContainedLoadingIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Exporting\u2026",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    /* -- CSV (primary export) -- */
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                exportStatus = null
                                val file = doToolsExport(context, db, proj, "csv") { pts, out ->
                                    CsvExporter.export(pts, out)
                                }
                                isExporting = false
                                if (file != null) {
                                    shareToolsFile(context, file, "text/csv")
                                    exportStatus = "CSV shared"
                                } else {
                                    exportStatus = "No points"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(percent = 50),
                    ) {
                        Icon(
                            Icons.Outlined.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Export CSV")
                    }

                    /* -- GeoJSON -- */
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                exportStatus = null
                                val file = doToolsExport(context, db, proj, "geojson") { pts, out ->
                                    GeoJsonExporter.export(pts, proj.name, out)
                                }
                                isExporting = false
                                if (file != null) {
                                    shareToolsFile(context, file, "application/geo+json")
                                    exportStatus = "GeoJSON shared"
                                } else {
                                    exportStatus = "No points"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Icon(
                            Icons.Outlined.Map,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Export GeoJSON")
                    }

                    /* -- DXF -- */
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                exportStatus = null
                                val file = doToolsExport(context, db, proj, "dxf") { pts, out ->
                                    DxfExporter.export(pts, out)
                                }
                                isExporting = false
                                if (file != null) {
                                    shareToolsFile(context, file, "application/dxf")
                                    exportStatus = "DXF shared"
                                } else {
                                    exportStatus = "No points"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Icon(
                            Icons.Outlined.Architecture,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Export DXF")
                    }

                    /* -- Shapefile (ZIP) -- */
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                exportStatus = null
                                val file = doToolsExport(context, db, proj, "zip") { pts, out ->
                                    ShapefileExporter.export(pts, proj.name, out)
                                }
                                isExporting = false
                                if (file != null) {
                                    shareToolsFile(context, file, "application/zip")
                                    exportStatus = "Shapefile shared"
                                } else {
                                    exportStatus = "No points"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Icon(
                            Icons.Outlined.Code,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Export Shapefile (ZIP)")
                    }
                }
            }

            /* ---- Export status chip ---- */
            exportStatus?.let { status ->
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            status,
                            fontFamily = CoordinateFont,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                )
            }
        }
    }
}

private suspend fun doToolsExport(
    context: Context, db: AppDatabase, project: ProjectEntity, ext: String,
    exporter: (List<PointEntity>, java.io.OutputStream) -> Unit,
): File? = withContext(Dispatchers.IO) {
    val points = db.pointDao().getByProjectOnce(project.id)
    if (points.isEmpty()) return@withContext null
    val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
    val file = File(dir, "${project.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.$ext")
    file.outputStream().use { exporter(points, it) }
    file
}

private fun shareToolsFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType; putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share survey data"))
}
