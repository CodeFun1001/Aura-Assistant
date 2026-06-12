package com.kastack.auraassistant.domain.repositories

import androidx.paging.PagingData
import com.kastack.auraassistant.domain.models.ChatMessage
import com.kastack.auraassistant.domain.models.Reminder
import com.kastack.auraassistant.domain.models.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun getUserProfile(): Flow<UserProfile>
    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun isOnboardingComplete(): Boolean
}

interface ChatRepository {
    fun getPagedMessages(): Flow<PagingData<ChatMessage>>
    suspend fun insertMessage(message: ChatMessage): Long
    fun getRecentMessages(limit: Int = 20): Flow<List<ChatMessage>>
    suspend fun markAsSynced(ids: List<Long>)
    fun getUnsyncedMessages(): Flow<List<ChatMessage>>
}

interface ReminderRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    fun getTodayReminders(): Flow<List<Reminder>>
    suspend fun insertReminder(reminder: Reminder): Long
    suspend fun markCompleted(id: Long)
    suspend fun deleteReminder(id: Long)
    fun getUnsyncedReminders(): Flow<List<Reminder>>
}