package org.opentopo.app.survey

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import org.opentopo.app.gnss.GnssState
import org.opentopo.transform.GeographicCoordinate
import org.opentopo.transform.HeposTransform
import java.io.InputStream
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Stakeout navigation — computes live delta to a target point.
 */
class Stakeout(
    private val gnssState: GnssState,
    gridDeStream: InputStream,
    gridDnStream: InputStream,
) {
    private val transform = HeposTransform(gridDeStream, gridDnStream)

    private val _target = MutableStateFlow<StakeoutTarget?>(null)

    fun setTarget(target: StakeoutTarget?) {
        _target.value = target
    }

    /** Flow of live stakeout results, updated whenever position or target changes. */
    val result: Flow<StakeoutResult?> = combine(
        gnssState.position,
        _target,
    ) { position, target ->
        if (target == null || !position.hasFix) return@combine null

        val currentProjected = transform.forward(
            GeographicCoordinate(position.latitude, position.longitude, position.altitude ?: 0.0)
        )

        val deltaE = target.easting - currentProjected.eastingM
        val deltaN = target.northing - currentProjected.northingM
        val distance = sqrt(deltaE * deltaE + deltaN * deltaN)
        val bearingRad = atan2(deltaE, deltaN)
        var bearingDeg = bearingRad * 180.0 / PI
        if (bearingDeg < 0) bearingDeg += 360.0

        StakeoutResult(
            target = target,
            currentEasting = currentProjected.eastingM,
            currentNorthing = currentProjected.northingM,
            deltaEasting = deltaE,
            deltaNorthing = deltaN,
            distance = distance,
            bearingDeg = bearingDeg,
        )
    }
}

data class StakeoutTarget(
    val name: String,
    val easting: Double,      // EGSA87 E
    val northing: Double,     // EGSA87 N
    val elevation: Double? = null,
)

data class StakeoutResult(
    val target: StakeoutTarget,
    val currentEasting: Double,
    val currentNorthing: Double,
    val deltaEasting: Double,
    val deltaNorthing: Double,
    val distance: Double,
    val bearingDeg: Double,
) {
    val bearingCardinal: String
        get() {
            val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val index = ((bearingDeg + 22.5) / 45.0).toInt() % 8
            return dirs[index]
        }
}
