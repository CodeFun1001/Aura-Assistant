package com.kastack.auraassistant.data.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kastack.auraassistant.data.datastore.SyncPreferences
import com.kastack.auraassistant.domain.models.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val workManager: WorkManager,
    private val syncPreferences: SyncPreferences
) {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    companion object {
        const val SYNC_WORK_NAME = "aura_sync_work"
    }

    fun requestSync() {
        _syncStatus.value = SyncStatus.Syncing

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15, TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun onSyncSuccess(at: Long) {
        _syncStatus.value = SyncStatus.Synced(at)
    }

    fun onSyncFailed(reason: String) {
        _syncStatus.value = SyncStatus.Failed(reason)
    }

    fun resetToIdle() {
        _syncStatus.value = SyncStatus.Idle
    }
}