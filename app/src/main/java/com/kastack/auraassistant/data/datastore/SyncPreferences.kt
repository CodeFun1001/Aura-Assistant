package com.kastack.auraassistant.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
    }

    suspend fun getLastSyncedAt(): Long =
        dataStore.data.first()[KEY_LAST_SYNCED_AT] ?: 0L

    suspend fun setLastSyncedAt(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNCED_AT] = timestamp
        }
    }
}