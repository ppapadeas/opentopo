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
