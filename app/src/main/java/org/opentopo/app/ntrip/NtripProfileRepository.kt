package org.opentopo.app.ntrip

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.opentopo.app.db.AppDatabase
import org.opentopo.app.prefs.UserPreferences
import java.util.UUID

/**
 * Repository that owns the list of [NtripProfile]s, the currently-active one,
 * and the derived [NtripConnectionState] surfaced to UI.
 *
 * Connects / disconnects the transport [NtripClient] automatically whenever
 * the active profile changes.
 */
class NtripProfileRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val ntripClient: NtripClient,
    private val prefs: UserPreferences,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) {
    private val dao = db.ntripProfileDao()

    /** All saved profiles, active-first, then by lastUsedAt DESC. */
    val profiles: Flow<List<NtripProfile>> = dao.observeAll()

    /** Currently-active profile, or null when none is marked. */
    val activeProfile: Flow<NtripProfile?> = dao.observeActive()

    /**
     * Derived high-level connection state for the Active Profile row.
     * Combines active profile with raw [NtripClient.state].
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: Flow<NtripConnectionState> = activeProfile.flatMapLatest { profile ->
        ntripClient.state.map { raw -> NtripConnectionState.from(raw, profile) }
    }

    init {
        // Auto-reconnect whenever the active profile changes while the app is running.
        externalScope.launch {
            activeProfile.collectDistinct { profile ->
                if (profile == null) {
                    ntripClient.disconnect()
                } else {
                    ntripClient.connect(profile.toConfig())
                }
            }
        }
    }

    /** One-shot seed for a fresh install or migration. Idempotent. */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return

        // Migrate the user's old single DataStore-backed NtripConfig into a
        // live profile, if it has anything interesting in it.
        val legacyHost = prefs.ntripHostOnce()
        val legacyUsername = prefs.ntripUsernameOnce()
        val legacyMountpoint = prefs.ntripMountpointOnce()

        val now = System.currentTimeMillis()
        val migrated = if (legacyHost.isNotBlank() || legacyUsername.isNotBlank()) {
            val name = when {
                legacyHost.contains("hepos", ignoreCase = true) -> "HEPOS · Nationwide VRS"
                legacyHost.contains("civilpos", ignoreCase = true) -> "CivilPOS · Metropolitan"
                legacyHost.contains("smartnet", ignoreCase = true) -> "SmartNet · Greek mainland"
                legacyHost.isNotBlank() -> legacyHost.substringBefore('.').replaceFirstChar { it.uppercaseChar() }
                else -> "Saved profile"
            }
            val code = NtripBadgePalette.deriveCode(name)
            val (bg, fg) = NtripBadgePalette.forCode(code)
            NtripProfile(
                id = UUID.randomUUID().toString(),
                displayName = name,
                code = code,
                tintColor = bg,
                badgeFgColor = fg,
                host = legacyHost,
                port = prefs.ntripPortOnce().toIntOrNull() ?: 2101,
                username = legacyUsername,
                password = prefs.ntripPasswordOnce(),
                mountpoint = legacyMountpoint,
                isActive = true,
                lastUsedAt = now,
                createdAt = now,
            )
        } else null

        // Shipping seed templates (no credentials yet).
        val templates = listOf(
            seedTemplate("HEPOS · Nationwide VRS", "rtk.hepos.gr", now),
            seedTemplate("CivilPOS · Metropolitan", "civilpos.gr", now),
            seedTemplate("SmartNet · Greek mainland", "ntrip.smartnet-eu.com", now),
        )

        if (migrated != null) {
            dao.upsert(migrated)
            // Avoid duplicating the migrated host in templates.
            templates
                .filter { !it.host.equals(migrated.host, ignoreCase = true) }
                .forEach { dao.upsert(it) }
        } else {
            templates.forEach { dao.upsert(it) }
        }
    }

    private fun seedTemplate(name: String, host: String, now: Long): NtripProfile {
        val code = NtripBadgePalette.deriveCode(name)
        val (bg, fg) = NtripBadgePalette.forCode(code)
        return NtripProfile(
            id = UUID.randomUUID().toString(),
            displayName = name,
            code = code,
            tintColor = bg,
            badgeFgColor = fg,
            host = host,
            port = 2101,
            username = "",
            password = "",
            mountpoint = "",
            isActive = false,
            lastUsedAt = 0L,
            createdAt = now,
        )
    }

    suspend fun setActive(id: String) {
        dao.setActive(id)
    }

    suspend fun upsert(profile: NtripProfile) {
        // Auto-derive code + palette if blank.
        val fixed = if (profile.code.isBlank()) {
            val derived = NtripBadgePalette.deriveCode(profile.displayName)
            val (bg, fg) = NtripBadgePalette.forCode(derived)
            profile.copy(code = derived, tintColor = bg, badgeFgColor = fg)
        } else {
            profile
        }
        dao.upsert(fixed)
    }

    suspend fun delete(id: String) = dao.deleteById(id)

    suspend fun duplicate(id: String): NtripProfile? {
        val src = dao.getById(id) ?: return null
        val copy = src.copy(
            id = UUID.randomUUID().toString(),
            displayName = src.displayName + " (copy)",
            isActive = false,
            createdAt = System.currentTimeMillis(),
            lastUsedAt = 0L,
        )
        dao.upsert(copy)
        return copy
    }

    suspend fun scanSourcetable(
        host: String,
        port: Int,
        username: String = "",
        password: String = "",
    ): Result<List<NtripMountpoint>> {
        val config = NtripConfig(
            name = "scan",
            host = host,
            port = port,
            username = username,
            password = password,
        )
        return kotlin.runCatching {
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                ntripClient.fetchSourcetable(config) { result ->
                    if (cont.isActive) {
                        result.fold(
                            onSuccess = { cont.resume(it) {} },
                            onFailure = { cont.resumeWith(Result.failure(it)) },
                        )
                    }
                }
            }
        }
    }

    /** Cached state flow for consumers that can't collect the raw Flow. */
    val stateSnapshot = state.stateIn(externalScope, SharingStarted.Eagerly, NtripConnectionState.Empty)
}

/**
 * Collect only distinct successive values — tiny helper to avoid bouncing the
 * NtripClient when the same active profile re-emits due to other field changes.
 */
private suspend fun <T> Flow<T>.collectDistinct(block: suspend (T) -> Unit) {
    var last: Any? = UNSET
    collect { value ->
        val prevId = (last as? NtripProfile?)?.id
        val nextId = (value as? NtripProfile?)?.id
        if (last === UNSET || prevId != nextId) {
            last = value
            block(value)
        }
    }
}

private val UNSET = Any()
