package com.kastack.auraassistant.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.kastack.auraassistant.data.datastore.UserProfileDataStore
import com.kastack.auraassistant.data.room.dao.ChatMessageDao
import com.kastack.auraassistant.data.room.dao.ReminderDao
import com.kastack.auraassistant.data.room.entities.ChatMessageEntity
import com.kastack.auraassistant.data.room.entities.ReminderEntity
import com.kastack.auraassistant.domain.models.ChatMessage
import com.kastack.auraassistant.domain.models.Reminder
import com.kastack.auraassistant.domain.models.UserProfile
import com.kastack.auraassistant.domain.repositories.ChatRepository
import com.kastack.auraassistant.domain.repositories.ReminderRepository
import com.kastack.auraassistant.domain.repositories.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    private val dataStore: UserProfileDataStore
) : UserProfileRepository {

    override fun getUserProfile(): Flow<UserProfile> = dataStore.userProfile

    override suspend fun saveUserProfile(profile: UserProfile) = dataStore.save(profile)

    override suspend fun isOnboardingComplete(): Boolean =
        dataStore.userProfile.first().isOnboardingComplete
}

class ChatRepositoryImpl @Inject constructor(
    private val dao: ChatMessageDao
) : ChatRepository {

    override fun getPagedMessages(): Flow<PagingData<ChatMessage>> =
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false)
        ) { dao.getPagedMessages() }
            .flow
            .map { pagingData -> pagingData.map { it.toDomain() } }

    override suspend fun insertMessage(message: ChatMessage): Long =
        dao.insertMessage(message.toEntity())

    override fun getRecentMessages(limit: Int): Flow<List<ChatMessage>> =
        dao.getRecentMessages(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun markAsSynced(ids: List<Long>) = dao.markAsSynced(ids)

    override fun getUnsyncedMessages(): Flow<List<ChatMessage>> =
        dao.getUnsyncedMessages().map { list -> list.map { it.toDomain() } }
}

class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<Reminder>> =
        dao.getAllReminders().map { list -> list.map { it.toDomain() } }

    override fun getTodayReminders(): Flow<List<Reminder>> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        val endOfDay = cal.timeInMillis
        return dao.getTodayReminders(startOfDay, endOfDay).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertReminder(reminder: Reminder): Long =
        dao.insertReminder(reminder.toEntity())

    override suspend fun markCompleted(id: Long) = dao.markCompleted(id)

    override fun getUnsyncedReminders(): Flow<List<Reminder>> =
        dao.getUnsyncedReminders().map { list -> list.map { it.toDomain() } }
}

fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id, content = content, sender = sender, timestamp = timestamp, meta = meta
)

fun ChatMessage.toEntity() = ChatMessageEntity(
    id = id, content = content, sender = sender, timestamp = timestamp, meta = meta
)

fun ReminderEntity.toDomain() = Reminder(
    id = id, title = title, scheduledAt = scheduledAt,
    isCompleted = isCompleted, createdAt = createdAt
)

fun Reminder.toEntity() = ReminderEntity(
    id = id, title = title, scheduledAt = scheduledAt,
    isCompleted = isCompleted, createdAt = createdAt
)