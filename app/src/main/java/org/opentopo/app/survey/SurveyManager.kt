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

    private var averagingJob: Job? = null

    /** Settings for point recording. */
    var averagingSeconds: Int = 5
    var minAccuracyM: Double = 0.0      // 0 = no filter
    var requireRtkFix: Boolean = false

    /**
     * Start recording a point. Collects epochs for [averagingSeconds],
     * then stores the averaged position.
     */
    fun startRecording(projectId: Long, remarks: String = "") {
        averagingJob?.cancel()
        _recordingState.value = RecordingState(isRecording = true, epochsCollected = 0)

        averagingJob = scope.launch {
            val epochs = mutableListOf<EpochSample>()
            val startTime = System.currentTimeMillis()

            while (isActive) {
                val pos = gnssState.position.value
                val acc = gnssState.accuracy.value

                if (pos.hasFix) {
                    // Quality filter
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
                if (elapsed >= averagingSeconds && epochs.isNotEmpty()) {
                    break
                }

                delay(1000) // sample once per second
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

        // Transform to EGSA87
        val projected = transform.forward(
            GeographicCoordinate(avgLat, avgLon, avgAlt ?: 0.0)
        )

        // Generate point ID
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
    val lastRecordedPoint: PointEntity? = null,
    val error: String? = null,
)
