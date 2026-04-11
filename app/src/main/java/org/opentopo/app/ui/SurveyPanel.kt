package org.opentopo.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.opentopo.app.db.AppDatabase
import org.opentopo.app.db.PointEntity
import org.opentopo.app.db.ProjectEntity
import org.opentopo.app.survey.RecordingState
import org.opentopo.app.survey.SurveyManager

@Composable
fun SurveyPanel(
    db: AppDatabase,
    surveyManager: SurveyManager?,
    modifier: Modifier = Modifier,
) {
    val projects by db.projectDao().getAll().collectAsState(initial = emptyList())
    var selectedProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var showNewProjectDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (selectedProject != null) {
            ProjectDetail(
                project = selectedProject!!,
                db = db,
                surveyManager = surveyManager,
                onBack = { selectedProject = null },
            )
        } else {
            Text("Survey", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showNewProjectDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("New Project") }

            Spacer(Modifier.height(8.dp))

            if (projects.isEmpty()) {
                Text("No projects yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                // Can't use LazyColumn inside bottom sheet scroll — use regular Column
                projects.forEach { project ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedProject = project },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(project.name, style = MaterialTheme.typography.titleSmall)
                            if (project.description.isNotBlank()) {
                                Text(project.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        if (showNewProjectDialog) {
            NewProjectDialog(db, onDismiss = { showNewProjectDialog = false }, onCreated = { showNewProjectDialog = false })
        }
    }
}

@Composable
private fun ProjectDetail(
    project: ProjectEntity,
    db: AppDatabase,
    surveyManager: SurveyManager?,
    onBack: () -> Unit,
) {
    val points by db.pointDao().getByProject(project.id).collectAsState(initial = emptyList())
    val recordingState = surveyManager?.recordingState?.collectAsState()?.value ?: RecordingState()
    var remarks by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(project.name, style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(8.dp))

        // Record controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))

                if (recordingState.isRecording) {
                    Text("Averaging... ${recordingState.epochsCollected} epochs")
                    OutlinedButton(
                        onClick = { surveyManager?.cancelRecording() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Cancel") }
                } else {
                    recordingState.lastRecordedPoint?.let { pt ->
                        Text(
                            "${pt.pointId}: E=${pt.easting?.let { "%.3f".format(it) }} N=${pt.northing?.let { "%.3f".format(it) }}",
                            style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    recordingState.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { surveyManager?.startRecording(project.id, remarks); remarks = "" },
                        enabled = surveyManager != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Record Point") }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Points (${points.size})", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))

        points.forEach { point ->
            PointRow(point)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PointRow(point: PointEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(point.pointId, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "E:${point.easting?.let { "%.3f".format(it) } ?: "\u2014"} N:${point.northing?.let { "%.3f".format(it) } ?: "\u2014"}",
                    style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                )
                val fix = when (point.fixQuality) { 4 -> "RTK"; 5 -> "Float"; 2 -> "DGPS"; 1 -> "GPS"; else -> "?" }
                Text("$fix ${point.remarks}".trim(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun NewProjectDialog(db: AppDatabase, onDismiss: () -> Unit, onCreated: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) { scope.launch { db.projectDao().insert(ProjectEntity(name = name, description = description)); onCreated() } } },
                enabled = name.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
