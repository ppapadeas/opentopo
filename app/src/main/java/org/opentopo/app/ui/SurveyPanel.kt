package org.opentopo.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Pentagon
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.launch
import org.opentopo.app.db.AppDatabase
import org.opentopo.app.db.PointEntity
import org.opentopo.app.db.ProjectEntity
import org.opentopo.app.survey.RecordingState
import org.opentopo.app.survey.SurveyManager
import org.opentopo.app.ui.components.TonalCard
import org.opentopo.app.ui.theme.CoordinateFont
import org.opentopo.app.ui.theme.LocalSurveyColors

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SurveyPanel(
    db: AppDatabase,
    surveyManager: SurveyManager?,
    modifier: Modifier = Modifier,
) {
    val activeProjectId by surveyManager?.activeProjectId?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val projects by db.projectDao().getAll().collectAsState(initial = emptyList())
    val activeProject = projects.find { it.id == activeProjectId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        if (activeProject != null) {
            ProjectDetail(
                project = activeProject,
                db = db,
                surveyManager = surveyManager,
            )
        } else {
            // No project selected — prompt user
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Outlined.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Text(
                    "Select a project from the header above",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Use the project dropdown to select or create a project.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Section header ──

@Composable
internal fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
    }
}

// ── Empty state ──

@Composable
private fun EmptyProjectsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.Inventory2,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Text(
            "No projects yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Create a project to start recording survey points.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

// ── Project card ──

@Composable
private fun ProjectCard(
    project: ProjectEntity,
    pointCount: Int?,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Folder,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (project.description.isNotBlank()) {
                    Text(
                        project.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            pointCount?.let { count ->
                Spacer(Modifier.width(8.dp))
                Text(
                    "$count pts",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = CoordinateFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Project detail ──

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProjectDetail(
    project: ProjectEntity,
    db: AppDatabase,
    surveyManager: SurveyManager?,
) {
    val points by db.pointDao().getByProject(project.id).collectAsState(initial = emptyList())
    val recordingState = surveyManager?.recordingState?.collectAsState()?.value ?: RecordingState()
    var remarks by remember { mutableStateOf("") }
    val activity = LocalContext.current as? org.opentopo.app.MainActivity
    val prefs = activity?.prefs
    val savedAH by prefs?.antennaHeight?.collectAsState(initial = "1.80") ?: remember { mutableStateOf("1.80") }
    var antennaHeight by remember { mutableStateOf(savedAH) }
    val scope = rememberCoroutineScope()

    // Sync when saved value loads
    LaunchedEffect(savedAH) { antennaHeight = savedAH }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Spacer(Modifier.height(8.dp))

        // Recording mode switcher
        val recordingMode by surveyManager?.recordingMode?.collectAsState() ?: remember { mutableStateOf("point") }
        val activeFeatureId by surveyManager?.activeFeatureId?.collectAsState() ?: remember { mutableStateOf(null) }
        val vertexCount by surveyManager?.vertexCount?.collectAsState() ?: remember { mutableStateOf(0) }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = recordingMode == "point",
                onClick = { surveyManager?.setRecordingMode("point") },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                icon = { SegmentedButtonDefaults.Icon(active = recordingMode == "point") { Icon(Icons.Outlined.LocationOn, null, Modifier.size(18.dp)) } },
            ) { Text("Point") }
            SegmentedButton(
                selected = recordingMode == "line",
                onClick = { surveyManager?.setRecordingMode("line") },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                icon = { SegmentedButtonDefaults.Icon(active = recordingMode == "line") { Icon(Icons.Outlined.Timeline, null, Modifier.size(18.dp)) } },
            ) { Text("Line") }
            SegmentedButton(
                selected = recordingMode == "polygon",
                onClick = { surveyManager?.setRecordingMode("polygon") },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                icon = { SegmentedButtonDefaults.Icon(active = recordingMode == "polygon") { Icon(Icons.Outlined.Pentagon, null, Modifier.size(18.dp)) } },
            ) { Text("Area") }
        }

        Spacer(Modifier.height(8.dp))

        // -- Recording controls --
        RecordingControls(
            project = project,
            surveyManager = surveyManager,
            recordingState = recordingState,
            antennaHeight = antennaHeight,
            onAntennaHeightChange = {
                antennaHeight = it
                scope.launch { prefs?.setAntennaHeight(it) }
            },
            remarks = remarks,
            onRemarksChange = { remarks = it },
            onRecorded = { remarks = "" },
        )

        if (recordingMode == "line" || recordingMode == "polygon") {
            Spacer(Modifier.height(8.dp))

            // Line/Polygon recording controls
            if (activeFeatureId == null) {
                // Not recording — show "Start" button
                val featureScope = rememberCoroutineScope()
                FilledTonalButton(
                    onClick = { featureScope.launch { surveyManager?.startFeature() } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(if (recordingMode == "line") Icons.Outlined.Timeline else Icons.Outlined.Pentagon, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start ${if (recordingMode == "line") "Line" else "Polygon"}")
                }
            } else {
                // Recording active — show vertex count + add vertex + finish
                TonalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Recording ${if (recordingMode == "line") "Line" else "Polygon"}", style = MaterialTheme.typography.titleMedium)
                        Text("$vertexCount vertices recorded", style = MaterialTheme.typography.bodyMedium, fontFamily = CoordinateFont)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { surveyManager?.recordVertex() },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Vertex")
                            }
                            OutlinedButton(
                                onClick = { surveyManager?.finishFeature() },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Finish")
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // -- Points section header --
        SectionHeader(
            icon = Icons.Outlined.RadioButtonChecked,
            title = "Points (${points.size})",
        )

        Spacer(Modifier.height(12.dp))

        if (points.isEmpty()) {
            Text(
                "No points recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 32.dp, top = 4.dp),
            )
        } else {
            points.forEach { point ->
                PointCard(point = point, db = db)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ── Recording controls ──

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RecordingControls(
    project: ProjectEntity,
    surveyManager: SurveyManager?,
    recordingState: RecordingState,
    antennaHeight: String,
    onAntennaHeightChange: (String) -> Unit,
    remarks: String,
    onRemarksChange: (String) -> Unit,
    onRecorded: () -> Unit,
) {
    val surveyColors = LocalSurveyColors.current

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Input fields
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = antennaHeight,
                    onValueChange = onAntennaHeightChange,
                    label = { Text("AH (m)") },
                    modifier = Modifier.weight(0.35f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                        .copy(fontFamily = CoordinateFont),
                )
                OutlinedTextField(
                    value = remarks,
                    onValueChange = onRemarksChange,
                    label = { Text("Remarks") },
                    modifier = Modifier.weight(0.65f),
                    singleLine = true,
                )
            }

            if (recordingState.isRecording) {
                // -- Recording in progress --
                Spacer(Modifier.height(4.dp))

                ContainedLoadingIndicator(
                    progress = { recordingState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    indicatorColor = surveyColors.recordingProgress,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Averaging: ${recordingState.epochsCollected}/${recordingState.totalEpochsTarget}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = CoordinateFont,
                        fontWeight = FontWeight.Bold,
                        color = surveyColors.recordingActive,
                    )
                    LoadingIndicator(modifier = Modifier.size(24.dp))
                }

                OutlinedButton(
                    onClick = { surveyManager?.cancelRecording() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cancel")
                }
            } else {
                // -- Last recorded point feedback --
                recordingState.lastRecordedPoint?.let { pt ->
                    Text(
                        "${pt.pointId}: E=${pt.easting?.let { "%.3f".format(it) }} N=${pt.northing?.let { "%.3f".format(it) }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = CoordinateFont,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // -- Error message --
                recordingState.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                // -- Record button --
                FilledTonalButton(
                    onClick = {
                        surveyManager?.antennaHeight = antennaHeight.toDoubleOrNull()
                        surveyManager?.startRecording(project.id, remarks)
                        onRecorded()
                    },
                    enabled = surveyManager != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Outlined.RadioButtonChecked,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Record Point")
                }
            }
        }
    }
}

// ── Point card ──

@Composable
private fun PointCard(point: PointEntity, db: AppDatabase) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Camera photo capture
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            scope.launch {
                db.pointDao().update(point.copy(photoPath = photoUri.value?.path))
            }
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Fix badge on the left
            Box(modifier = Modifier.width(56.dp)) {
                FixTypeBadge(point.fixQuality)
            }

            Spacer(Modifier.width(8.dp))

            // Coordinates + metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    point.pointId,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = CoordinateFont,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "E: ${point.easting?.let { "%.3f".format(it) } ?: "\u2014"}  N: ${point.northing?.let { "%.3f".format(it) } ?: "\u2014"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = CoordinateFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    point.horizontalAccuracy?.let {
                        AccuracyBadge(it, "H")
                    }
                    if (point.remarks.isNotBlank()) {
                        Text(
                            point.remarks,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Photo button
            IconButton(
                onClick = {
                    val photoFile = File(
                        context.filesDir,
                        "photos/${point.pointId}_${System.currentTimeMillis()}.jpg",
                    )
                    photoFile.parentFile?.mkdirs()
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile,
                    )
                    photoUri.value = uri
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = "Take photo for ${point.pointId}",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Edit button
            IconButton(
                onClick = { showEditDialog = true },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit ${point.pointId}",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Delete button
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete ${point.pointId}",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete point ${point.pointId}?") },
            text = {
                Text("This will permanently remove this survey point.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { db.pointDao().delete(point) }
                        showDeleteDialog = false
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Edit dialog
    if (showEditDialog) {
        EditPointDialog(
            point = point,
            onDismiss = { showEditDialog = false },
            onSave = { newRemarks, newAH ->
                scope.launch {
                    db.pointDao().update(
                        point.copy(remarks = newRemarks, antennaHeight = newAH),
                    )
                }
                showEditDialog = false
            },
        )
    }
}

// ── Edit point dialog ──

@Composable
private fun EditPointDialog(
    point: PointEntity,
    onDismiss: () -> Unit,
    onSave: (remarks: String, antennaHeight: Double?) -> Unit,
) {
    var remarks by remember { mutableStateOf(point.remarks) }
    var antennaHeight by remember {
        mutableStateOf(point.antennaHeight?.let { "%.2f".format(it) } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text("Edit ${point.pointId}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = antennaHeight,
                    onValueChange = { antennaHeight = it },
                    label = { Text("Antenna Height (m)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium
                        .copy(fontFamily = CoordinateFont),
                )
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(remarks, antennaHeight.toDoubleOrNull())
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

// ── New project dialog ──

@Composable
private fun NewProjectDialog(
    db: AppDatabase,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text("New Project")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        scope.launch {
                            db.projectDao().insert(
                                ProjectEntity(name = name, description = description),
                            )
                            onCreated()
                        }
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
