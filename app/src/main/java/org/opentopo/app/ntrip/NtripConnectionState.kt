package org.opentopo.app.ntrip

/**
 * High-level NTRIP connection state, mapped from the low-level [NtripState]
 * that [NtripClient] emits. Drives the Active-Profile row visuals on the
 * Connect screen (Live/Connecting/Stale/Disconnected/Error/Empty).
 */
sealed interface NtripConnectionState {
    /** No profile configured at all — show "Set up NTRIP corrections" empty row. */
    data object Empty : NtripConnectionState

    /** Profile is saved but the caster isn't actively connected. */
    data object Disconnected : NtripConnectionState

    /** Caster handshake in progress. */
    data object Connecting : NtripConnectionState

    /** Caster is delivering RTCM frames. `ageSeconds` is since the last frame. */
    data class Live(
        val ageSeconds: Double,
        val bitrate: Int,
    ) : NtripConnectionState

    /** Connected but no fresh corrections for more than [STALE_THRESHOLD_SECONDS]. */
    data class Stale(val ageSeconds: Double) : NtripConnectionState

    data class Error(val reason: String) : NtripConnectionState

    companion object {
        /** Correction age past which Live transitions to Stale. */
        const val STALE_THRESHOLD_SECONDS: Double = 10.0

        /**
         * Derive a high-level state from the raw [NtripState] emitted by the
         * transport layer plus the current [activeProfile]. `nowMillis` is
         * injected so the mapping is deterministic under test.
         */
        fun from(
            raw: NtripState,
            activeProfile: NtripProfile?,
            nowMillis: Long = System.currentTimeMillis(),
        ): NtripConnectionState {
            if (activeProfile == null) return Empty
            raw.error?.let { return Error(it) }
            return when (raw.status) {
                NtripStatus.DISCONNECTED -> Disconnected
                NtripStatus.CONNECTING, NtripStatus.RECONNECTING -> Connecting
                NtripStatus.CONNECTED -> {
                    val ageSec = if (raw.lastDataTime > 0) {
                        (nowMillis - raw.lastDataTime) / 1000.0
                    } else {
                        0.0
                    }
                    val bitrate = raw.dataRateBps.toInt()
                    if (ageSec >= STALE_THRESHOLD_SECONDS) {
                        Stale(ageSec)
                    } else {
                        Live(ageSec, bitrate)
                    }
                }
            }
        }
    }
}
