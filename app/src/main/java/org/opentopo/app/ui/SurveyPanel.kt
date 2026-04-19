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
import androidx.compose.material.icons.outlined.Pentagon
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.launch
import org.opentopo.app.db.AppDatabase
import org.opentopo.app.db.PointEntity
import org.opentopo.app.db.ProjectEntity
import org.opentopo.app.survey.RecordingState
import org.opentopo.app.survey.SurveyManager
import org.opentopo.app.ui.components.ButtonGroup as OpenTopoButtonGroup
import org.opentopo.app.ui.components.CoordinateBlock
import org.opentopo.app.ui.components.FixStatusPill
import org.opentopo.app.ui.components.InputRow
import org.opentopo.app.ui.components.SectionLabel
import org.opentopo.app.ui.components.TonalCard
import org.opentopo.app.ui.components.survey.AccuracyConvergenceRing
import org.opentopo.app.ui.components.survey.EpochAveragingBar
import org.opentopo.app.ui.components.survey.RecordButton
import org.opentopo.app.ui.components.survey.SplitButton
import org.opentopo.app.ui.theme.CoordinateFont

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

// ── Project detail (v2 layout) ──

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
    val savedAH by prefs?.antennaHeight?.collectAsState(initial = "1.80")
        ?: remember { mutableStateOf("1.80") }
    var antennaHeight by remember { mutableStateOf(savedAH) }
    val scope = rememberCoroutineScope()

    // Sync when saved value loads
    LaunchedEffect(savedAH) { antennaHeight = savedAH }

    // Recording mode switcher state
    val recordingMode by surveyManager?.recordingMode?.collectAsState()
        ?: remember { mutableStateOf("point") }
    val activeFeatureId by surveyManager?.activeFeatureId?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val vertexCount by surveyManager?.vertexCount?.collectAsState()
        ?: remember { mutableStateOf(0) }

    // Live EGSA87 projected coordinates from SurveyManager
    val projected by surveyManager?.projectedPosition?.collectAsState()
        ?: remember { mutableStateOf(null) }

    // Derive current fix/σH/sats from the last recorded epoch (the only live
    // signal reachable from SurveyPanel without touching MainActivity).
    val lastPt = recordingState.lastRecordedPoint
    val currentFix: Int = lastPt?.fixQuality ?: 0
    val sigmaH: Double? = lastPt?.horizontalAccuracy
    val numSats: Int = lastPt?.numSatellites ?: 0

    // Accuracy gate — backed by UserPreferences.minAccuracyM (DataStore) so
    // it survives process death and stays in sync with SettingsPanel. Pushed
    // to SurveyManager.minAccuracyM so it filters epochs during averaging.
    val gateString by prefs?.minAccuracyM?.collectAsState(initial = "0.02")
        ?: remember { mutableStateOf("0.02") }
    val accuracyGateSigma: Double = gateString.toDoubleOrNull() ?: 0.02
    var showGateMenu by remember { mutableStateOf(false) }
    LaunchedEffect(accuracyGateSigma) {
        surveyManager?.minAccuracyM = accuracyGateSigma
    }

    // Record button is only armed when the fix is RTK Fix (quality 4) and
    // the current σH clears the gate. When we have no live σH yet, we still
    // let the user kick off averaging so the session can collect samples —
    // SurveyManager will filter unqualified epochs internally.
    val recordEnabled: Boolean = surveyManager != null &&
        (lastPt == null || (currentFix >= 4 && (sigmaH ?: Double.MAX_VALUE) <= accuracyGateSigma))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Spacer(Modifier.height(8.dp))

        // 1. Top status strip ── fix pill (left) + σH convergence ring (right)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val sigmaExtras = sigmaH?.let {
                "${"%.1f".format(it * 100)} cm · ${numSats} sats"
            }
            FixStatusPill(fixQuality = currentFix, extras = sigmaExtras)
            AccuracyConvergenceRing(
                currentSigmaH = sigmaH,
                targetSigmaH = accuracyGateSigma,
                diameter = 72.dp,
            )
        }

        Spacer(Modifier.height(12.dp))

        // 2. Survey mode selector (OpenTopo ButtonGroup)
        val modes = listOf("Point", "Line", "Polygon")
        val modeKeys = listOf("point", "line", "polygon")
        val currentModeIndex = modeKeys.indexOf(recordingMode).coerceAtLeast(0)
        OpenTopoButtonGroup(
            options = modes,
            selectedIndex = currentModeIndex,
            onSelect = { idx -> surveyManager?.setRecordingMode(modeKeys[idx]) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        // 3. Live coordinates card (EGSA87)
        CoordinateBlock(
            label = "EGSA87 · EPSG 2100",
            easting = projected?.eastingM?.let { "${"%.3f".format(it)} m" } ?: "—",
            northing = projected?.northingM?.let { "${"%.3f".format(it)} m" } ?: "—",
            height = lastPt?.orthometricHeight?.let { "${"%.3f".format(it)} m" },
            modifier = Modifier.fillMaxWidth(),
            fixQuality = currentFix,
            sigmaH = sigmaH?.let { "σH ${"%.3f".format(it)} m" },
        )

        Spacer(Modifier.height(12.dp))

        // 4. Metadata inputs
        SectionLabel("Point metadata")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InputRow(
                label = "Description",
                value = remarks,
                onValueChange = { remarks = it },
                placeholder = "Remarks",
            )
            InputRow(
                label = "AH (m)",
                value = antennaHeight,
                onValueChange = {
                    antennaHeight = it
                    scope.launch { prefs?.setAntennaHeight(it) }
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        // 5. Epoch averaging bar — always shown so surveyors see target state
        EpochAveragingBar(
            epochsCollected = recordingState.epochsCollected,
            epochsTarget = recordingState.totalEpochsTarget,
            currentSigmaH = sigmaH,
            targetSigmaH = accuracyGateSigma,
            modifier = Modifier.fillMaxWidth(),
        )

        // -- Line/polygon controls (unchanged behaviour, preserved for v2) --
        if (recordingMode == "line" || recordingMode == "polygon") {
            Spacer(Modifier.height(12.dp))

            if (activeFeatureId == null) {
                val featureScope = rememberCoroutineScope()
                FilledTonalButton(
                    onClick = { featureScope.launch { surveyManager?.startFeature() } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(percent = 50),
                ) {
                    Icon(
                        if (recordingMode == "line") Icons.Outlined.Timeline else Icons.Outlined.Pentagon,
                        null,
                        Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Start ${if (recordingMode == "line") "Line" else "Polygon"}")
                }
            } else {
                TonalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Recording ${if (recordingMode == "line") "Line" else "Polygon"}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "$vertexCount vertices recorded",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = CoordinateFont,
                        )

                        var measurement by remember { mutableStateOf("") }
                        LaunchedEffect(vertexCount) {
                            if (vertexCount >= 2 && recordingMode == "line") {
                                val dist = surveyManager?.computeLineDistance(activeFeatureId!!)
                                measurement = if (dist != null) "%.2f m".format(dist) else ""
                            } else if (vertexCount >= 3 && recordingMode == "polygon") {
                                val area = surveyManager?.computePolygonArea(activeFeatureId!!)
                                if (area != null) {
                                    val stremma = area / 1000.0
                                    measurement = "%.1f m\u00B2 (%.3f stremma)".format(area, stremma)
                                }
                            } else {
                                measurement = ""
                            }
                        }

                        if (measurement.isNotBlank()) {
                            Text(
                                measurement,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = CoordinateFont,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (vertexCount > 0) {
                                IconButton(onClick = { surveyManager?.undoLastVertex() }) {
                                    Icon(Icons.Outlined.Undo, "Undo last vertex", Modifier.size(20.dp))
                                }
                            }
                            FilledTonalButton(
                                onClick = { surveyManager?.recordVertex() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(percent = 50),
                            ) {
                                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Vertex")
                            }
                            OutlinedButton(
                                onClick = { surveyManager?.finishFeature() },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.extraLarge,
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

        // Error feedback, if any
        recordingState.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }

        // 6. Record action row — RecordButton + SplitButton with gate dropdown
        val onRecord: () -> Unit = {
            if (recordingState.isRecording) {
                surveyManager?.cancelRecording()
            } else if (surveyManager != null) {
                surveyManager.antennaHeight = antennaHeight.toDoubleOrNull()
                surveyManager.startRecording(project.id, remarks)
                remarks = ""
            }
            Unit
        }
        val progressFraction: Float = (recordingState.epochsCollected.toFloat() /
            recordingState.totalEpochsTarget.toFloat().coerceAtLeast(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            RecordButton(
                onClick = onRecord,
                enabled = recordEnabled || recordingState.isRecording,
                isRecording = recordingState.isRecording,
                progress = progressFraction,
            )
            Box(modifier = Modifier.weight(1f)) {
                SplitButton(
                    primaryLabel = if (recordingState.isRecording) "Cancel" else "Record epoch",
                    onPrimaryClick = onRecord,
                    secondaryLabel = "\u00B1${(accuracyGateSigma * 100).toInt()} cm",
                    onSecondaryClick = { showGateMenu = true },
                    enabled = surveyManager != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = showGateMenu,
                    onDismissRequest = { showGateMenu = false },
                ) {
                    listOf("0.01", "0.02", "0.05").forEach { gate ->
                        DropdownMenuItem(
                            text = { Text("\u00B1${(gate.toDouble() * 100).toInt()} cm") },
                            onClick = {
                                scope.launch { prefs?.setMinAccuracyM(gate) }
                                showGateMenu = false
                            },
                        )
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
                // Height info
                val heightText = buildString {
                    point.orthometricHeight?.let { append("H=${"%.3f".format(it)}") }
                    point.geoidSeparation?.let { append(" N=${"%.2f".format(it)}") }
                }
                if (heightText.isNotBlank()) {
                    Text(
                        heightText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = CoordinateFont,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
