package org.opentopo.app.survey

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.opentopo.app.db.AppDatabase
import org.opentopo.app.db.PointEntity
import org.opentopo.app.gnss.GnssState
import org.opentopo.transform.GeographicCoordinate
import org.opentopo.transform.HeposTransform
import org.opentopo.transform.ProjectedCoordinate
import java.io.InputStream

/**
 * Manages survey point recording with epoch averaging and quality filtering.
 */
class SurveyManager(
    private val db: AppDatabase,
    private val gnssState: GnssState,
    gridDeStream: InputStream,
    gridDnStream: InputStream,
) {
    private val transform = HeposTransform(gridDeStream, gridDnStream)
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _activeProjectId = MutableStateFlow<Long?>(null)
    val activeProjectId: StateFlow<Long?> = _activeProjectId.asStateFlow()

    /** Live EGSA87 coordinates from current GNSS position. */
    private val _projectedPosition = MutableStateFlow<ProjectedCoordinate?>(null)
    val projectedPosition: StateFlow<ProjectedCoordinate?> = _projectedPosition.asStateFlow()

    // Active recording mode
    private val _recordingMode = MutableStateFlow("point") // "point", "line", "polygon"
    val recordingMode: StateFlow<String> = _recordingMode.asStateFlow()

    // Active feature being recorded (line or polygon)
    private val _activeFeatureId = MutableStateFlow<Long?>(null)
    val activeFeatureId: StateFlow<Long?> = _activeFeatureId.asStateFlow()

    // Vertex count for active feature
    private val _vertexCount = MutableStateFlow(0)
    val vertexCount: StateFlow<Int> = _vertexCount.asStateFlow()

    private var averagingJob: Job? = null

    /** Settings for point recording. */
    var averagingSeconds: Int = 5
    var minAccuracyM: Double = 0.0
    var requireRtkFix: Boolean = false
    var antennaHeight: Double? = null

    init {
        // Continuously transform live position to EGSA87
        scope.launch(Dispatchers.Default) {
            gnssState.position.collect { pos ->
                _projectedPosition.value = if (pos.hasFix) {
                    try {
                        transform.forward(GeographicCoordinate(pos.latitude, pos.longitude, pos.altitude ?: 0.0))
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }
            }
        }
    }

    fun setActiveProject(id: Long?) {
        _activeProjectId.value = id
    }

    /** Quick-record for FAB: uses active project with full averaging. */
    fun startRecording(remarks: String = "") {
        val projectId = _activeProjectId.value ?: return
        startRecording(projectId, remarks)
    }

    /** Quick mark: single-epoch instant capture, no averaging. */
    fun quickMark(remarks: String = "") {
        val projectId = _activeProjectId.value ?: return
        startRecording(projectId, remarks, overrideEpochs = 1)
    }

    fun startRecording(projectId: Long, remarks: String = "", overrideEpochs: Int = 0) {
        val targetEpochs = if (overrideEpochs > 0) overrideEpochs else averagingSeconds
        averagingJob?.cancel()
        _recordingState.value = RecordingState(
            isRecording = true,
            epochsCollected = 0,
            totalEpochsTarget = targetEpochs,
        )

        averagingJob = scope.launch {
            val epochs = mutableListOf<EpochSample>()
            val startTime = System.currentTimeMillis()

            while (isActive) {
                val pos = gnssState.position.value
                val acc = gnssState.accuracy.value

                if (pos.hasFix) {
                    val passesQuality = when {
                        requireRtkFix && pos.fixQuality != 4 -> false
                        minAccuracyM > 0 && (acc.horizontalAccuracyM ?: 999.0) > minAccuracyM -> false
                        else -> true
                    }

                    if (passesQuality) {
                        epochs.add(
                            EpochSample(
                                latitude = pos.latitude,
                                longitude = pos.longitude,
                                altitude = pos.altitude,
                                horizontalAccuracy = acc.horizontalAccuracyM,
                                verticalAccuracy = acc.altitudeErrorM,
                                fixQuality = pos.fixQuality,
                                numSatellites = pos.numSatellites,
                                hdop = acc.hdop,
                            )
                        )
                        _recordingState.value = _recordingState.value.copy(
                            epochsCollected = epochs.size,
                        )
                    }
                }

                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                if (elapsed >= targetEpochs && epochs.isNotEmpty()) {
                    break
                }

                delay(1000)
            }

            if (epochs.isNotEmpty()) {
                val point = averageAndStore(projectId, epochs, remarks)
                _recordingState.value = RecordingState(
                    isRecording = false,
                    lastRecordedPoint = point,
                )
            } else {
                _recordingState.value = RecordingState(
                    isRecording = false,
                    error = "No valid epochs collected",
                )
            }
        }
    }

    fun cancelRecording() {
        averagingJob?.cancel()
        _recordingState.value = RecordingState()
    }

    fun setRecordingMode(mode: String) {
        _recordingMode.value = mode
    }

    /** Start a new line or polygon feature. Returns the featureId. */
    suspend fun startFeature(): Long {
        val projectId = _activeProjectId.value ?: return -1
        val maxId = db.pointDao().getMaxFeatureId(projectId) ?: 0
        val featureId = maxId + 1
        _activeFeatureId.value = featureId
        _vertexCount.value = 0
        return featureId
    }

    /** Record a vertex for the active line/polygon. */
    fun recordVertex(remarks: String = "") {
        val projectId = _activeProjectId.value ?: return
        val featureId = _activeFeatureId.value ?: return
        val mode = _recordingMode.value
        val layerType = if (mode == "line") "line_vertex" else "polygon_vertex"

        // Use quick mark (1 epoch) for vertices
        scope.launch {
            val pos = gnssState.position.value
            val acc = gnssState.accuracy.value
            if (!pos.hasFix) return@launch

            val projected = try {
                transform.forward(GeographicCoordinate(pos.latitude, pos.longitude, pos.altitude ?: 0.0))
            } catch (_: Exception) { null }

            val vertexNum = _vertexCount.value + 1
            val pointId = if (mode == "line") "L${featureId}-V${vertexNum}" else "PG${featureId}-V${vertexNum}"

            val point = PointEntity(
                projectId = projectId,
                pointId = pointId,
                latitude = pos.latitude,
                longitude = pos.longitude,
                altitude = pos.altitude,
                easting = projected?.eastingM,
                northing = projected?.northingM,
                horizontalAccuracy = acc.horizontalAccuracyM,
                verticalAccuracy = acc.altitudeErrorM,
                fixQuality = pos.fixQuality,
                numSatellites = pos.numSatellites,
                hdop = acc.hdop ?: pos.hdop,
                averagingSeconds = 1,
                antennaHeight = antennaHeight,
                remarks = remarks,
                layerType = layerType,
                featureId = featureId,
            )
            db.pointDao().insert(point)
            _vertexCount.value = vertexNum
        }
    }

    /** Remove the last recorded vertex for the active feature. */
    fun undoLastVertex() {
        val featureId = _activeFeatureId.value ?: return
        scope.launch {
            val vertices = db.pointDao().getByFeatureOnce(featureId)
            if (vertices.isNotEmpty()) {
                db.pointDao().delete(vertices.last())
                _vertexCount.value = (_vertexCount.value - 1).coerceAtLeast(0)
            }
        }
    }

    /** Finish the active line/polygon feature. */
    fun finishFeature() {
        _activeFeatureId.value = null
        _vertexCount.value = 0
    }

    /** Compute the total distance of a line feature in meters. */
    suspend fun computeLineDistance(featureId: Long): Double {
        val vertices = db.pointDao().getByFeatureOnce(featureId)
        if (vertices.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until vertices.size) {
            val prev = vertices[i - 1]
            val curr = vertices[i]
            total += haversineDistance(prev.latitude, prev.longitude, curr.latitude, curr.longitude)
        }
        return total
    }

    /** Compute the area of a polygon feature in square meters (Shoelace formula on projected coords). */
    suspend fun computePolygonArea(featureId: Long): Double {
        val vertices = db.pointDao().getByFeatureOnce(featureId)
        if (vertices.size < 3) return 0.0
        // Use EGSA87 projected coordinates for area (meters)
        val coords = vertices.mapNotNull { pt ->
            if (pt.easting != null && pt.northing != null) pt.easting to pt.northing else null
        }
        if (coords.size < 3) return 0.0
        // Shoelace formula
        var area = 0.0
        for (i in coords.indices) {
            val j = (i + 1) % coords.size
            area += coords[i].first * coords[j].second
            area -= coords[j].first * coords[i].second
        }
        return kotlin.math.abs(area) / 2.0
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2).let { it * it } +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2).let { it * it }
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return R * c
    }

    private suspend fun averageAndStore(
        projectId: Long,
        epochs: List<EpochSample>,
        remarks: String,
    ): PointEntity {
        val avgLat = epochs.map { it.latitude }.average()
        val avgLon = epochs.map { it.longitude }.average()
        val avgAlt = epochs.mapNotNull { it.altitude }.takeIf { it.isNotEmpty() }?.average()
        val avgHAcc = epochs.mapNotNull { it.horizontalAccuracy }.takeIf { it.isNotEmpty() }?.average()
        val avgVAcc = epochs.mapNotNull { it.verticalAccuracy }.takeIf { it.isNotEmpty() }?.average()
        val bestFix = epochs.maxOf { it.fixQuality }
        val avgSats = epochs.map { it.numSatellites }.average().toInt()
        val avgHdop = epochs.mapNotNull { it.hdop }.takeIf { it.isNotEmpty() }?.average()

        val projected = transform.forward(
            GeographicCoordinate(avgLat, avgLon, avgAlt ?: 0.0)
        )

        val count = db.pointDao().countByProject(projectId)
        val pointId = "P%03d".format(count + 1)

        val point = PointEntity(
            projectId = projectId,
            pointId = pointId,
            latitude = avgLat,
            longitude = avgLon,
            altitude = avgAlt,
            easting = projected.eastingM,
            northing = projected.northingM,
            horizontalAccuracy = avgHAcc,
            verticalAccuracy = avgVAcc,
            fixQuality = bestFix,
            numSatellites = avgSats,
            hdop = avgHdop,
            averagingSeconds = epochs.size,
            antennaHeight = antennaHeight,
            remarks = remarks,
        )

        val id = db.pointDao().insert(point)
        return point.copy(id = id)
    }
}

private data class EpochSample(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val horizontalAccuracy: Double?,
    val verticalAccuracy: Double?,
    val fixQuality: Int,
    val numSatellites: Int,
    val hdop: Double?,
)

data class RecordingState(
    val isRecording: Boolean = false,
    val epochsCollected: Int = 0,
    val totalEpochsTarget: Int = 5,
    val lastRecordedPoint: PointEntity? = null,
    val error: String? = null,
) {
    val progress: Float
        get() = if (totalEpochsTarget > 0)
            (epochsCollected.toFloat() / totalEpochsTarget).coerceIn(0f, 1f) else 0f
}
