package org.opentopo.app.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "opentopo_prefs")

/**
 * Persists user connection settings across app restarts using DataStore.
 */
class UserPreferences(private val context: Context) {

    // ── GNSS connection ──
    private val KEY_CONNECTION_TYPE = intPreferencesKey("connection_type")
    private val KEY_BT_DEVICE_ADDRESS = stringPreferencesKey("bt_device_address")
    private val KEY_BT_DEVICE_NAME = stringPreferencesKey("bt_device_name")

    // ── NTRIP ──
    private val KEY_NTRIP_PRESET_INDEX = intPreferencesKey("ntrip_preset_index")
    private val KEY_NTRIP_HOST = stringPreferencesKey("ntrip_host")
    private val KEY_NTRIP_PORT = stringPreferencesKey("ntrip_port")
    private val KEY_NTRIP_USERNAME = stringPreferencesKey("ntrip_username")
    private val KEY_NTRIP_PASSWORD = stringPreferencesKey("ntrip_password")
    private val KEY_NTRIP_MOUNTPOINT = stringPreferencesKey("ntrip_mountpoint")

    // ── Survey ──
    private val KEY_ANTENNA_HEIGHT = stringPreferencesKey("antenna_height")

    // ── Flows ──

    val connectionType: Flow<Int> = context.dataStore.data.map { it[KEY_CONNECTION_TYPE] ?: 0 }
    val btDeviceAddress: Flow<String> = context.dataStore.data.map { it[KEY_BT_DEVICE_ADDRESS] ?: "" }
    val btDeviceName: Flow<String> = context.dataStore.data.map { it[KEY_BT_DEVICE_NAME] ?: "" }
    val ntripPresetIndex: Flow<Int> = context.dataStore.data.map { it[KEY_NTRIP_PRESET_INDEX] ?: 0 }
    val ntripHost: Flow<String> = context.dataStore.data.map { it[KEY_NTRIP_HOST] ?: "" }
    val ntripPort: Flow<String> = context.dataStore.data.map { it[KEY_NTRIP_PORT] ?: "2101" }
    val ntripUsername: Flow<String> = context.dataStore.data.map { it[KEY_NTRIP_USERNAME] ?: "" }
    val ntripPassword: Flow<String> = context.dataStore.data.map { it[KEY_NTRIP_PASSWORD] ?: "" }
    val ntripMountpoint: Flow<String> = context.dataStore.data.map { it[KEY_NTRIP_MOUNTPOINT] ?: "" }
    val antennaHeight: Flow<String> = context.dataStore.data.map { it[KEY_ANTENNA_HEIGHT] ?: "1.80" }

    // ── Setters ──

    suspend fun setConnectionType(value: Int) {
        context.dataStore.edit { it[KEY_CONNECTION_TYPE] = value }
    }

    suspend fun setBtDevice(address: String, name: String) {
        context.dataStore.edit {
            it[KEY_BT_DEVICE_ADDRESS] = address
            it[KEY_BT_DEVICE_NAME] = name
        }
    }

    suspend fun setNtripConfig(
        presetIndex: Int,
        host: String,
        port: String,
        username: String,
        password: String,
        mountpoint: String,
    ) {
        context.dataStore.edit {
            it[KEY_NTRIP_PRESET_INDEX] = presetIndex
            it[KEY_NTRIP_HOST] = host
            it[KEY_NTRIP_PORT] = port
            it[KEY_NTRIP_USERNAME] = username
            it[KEY_NTRIP_PASSWORD] = password
            it[KEY_NTRIP_MOUNTPOINT] = mountpoint
        }
    }

    suspend fun setAntennaHeight(value: String) {
        context.dataStore.edit { it[KEY_ANTENNA_HEIGHT] = value }
    }
}
