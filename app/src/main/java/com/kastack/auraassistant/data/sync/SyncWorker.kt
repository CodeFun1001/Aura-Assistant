package com.kastack.auraassistant.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kastack.auraassistant.data.datastore.SyncPreferences
import com.kastack.auraassistant.domain.repositories.ChatRepository
import com.kastack.auraassistant.domain.repositories.ReminderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val chatRepository: ChatRepository,
    private val reminderRepository: ReminderRepository,
    private val syncPreferences: SyncPreferences,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()

            val unsyncedMessages = chatRepository.getUnsyncedMessages().first()
            val unsyncedReminders = reminderRepository.getUnsyncedReminders().first()

            pushMessages(unsyncedMessages)
            pushReminders(unsyncedReminders)

            if (unsyncedMessages.isNotEmpty()) {
                chatRepository.markAsSynced(unsyncedMessages.map { it.id })
            }

            syncPreferences.setLastSyncedAt(now)
            syncManager.onSyncSuccess(now)

            Result.success()
        } catch (e: Exception) {
            syncManager.onSyncFailed(e.message ?: "Unknown sync error")
            Result.retry()
        }
    }

    private suspend fun pushMessages(
        messages: List<com.kastack.auraassistant.domain.models.ChatMessage>
    ) {
    }

    private suspend fun pushReminders(
        reminders: List<com.kastack.auraassistant.domain.models.Reminder>
    ) {
    }
}