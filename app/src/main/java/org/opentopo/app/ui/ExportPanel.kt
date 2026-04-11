package org.opentopo.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opentopo.app.db.AppDatabase
import org.opentopo.app.db.PointEntity
import org.opentopo.app.db.ProjectEntity
import org.opentopo.app.export.CsvExporter
import org.opentopo.app.export.DxfExporter
import org.opentopo.app.export.GeoJsonExporter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPanel(
    db: AppDatabase,
    modifier: Modifier = Modifier,
) {
    val projects by db.projectDao().getAll().collectAsState(initial = emptyList())
    var selectedProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("Export", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            TextField(
                value = selectedProject?.name ?: "Select project...",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                projects.forEach { project ->
                    DropdownMenuItem(
                        text = { Text(project.name) },
                        onClick = { selectedProject = project; expanded = false; exportStatus = null },
                    )
                }
                if (projects.isEmpty()) {
                    DropdownMenuItem(text = { Text("No projects") }, onClick = { expanded = false })
                }
            }
        }

        val proj = selectedProject
        if (proj != null) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = {
                    scope.launch {
                        val file = doExport(context, db, proj, "csv") { pts, out -> CsvExporter.export(pts, out) }
                        if (file != null) { shareFile(context, file, "text/csv"); exportStatus = "CSV shared" }
                        else exportStatus = "No points"
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Export CSV") }

                Button(onClick = {
                    scope.launch {
                        val file = doExport(context, db, proj, "geojson") { pts, out -> GeoJsonExporter.export(pts, proj.name, out) }
                        if (file != null) { shareFile(context, file, "application/geo+json"); exportStatus = "GeoJSON shared" }
                        else exportStatus = "No points"
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Export GeoJSON") }

                Button(onClick = {
                    scope.launch {
                        val file = doExport(context, db, proj, "dxf") { pts, out -> DxfExporter.export(pts, out) }
                        if (file != null) { shareFile(context, file, "application/dxf"); exportStatus = "DXF shared" }
                        else exportStatus = "No points"
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Export DXF") }
            }
        }

        exportStatus?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = org.opentopo.app.ui.theme.CoordinateFont)
        }
    }
}

private suspend fun doExport(
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

private fun shareFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType; putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share survey data"))
}
