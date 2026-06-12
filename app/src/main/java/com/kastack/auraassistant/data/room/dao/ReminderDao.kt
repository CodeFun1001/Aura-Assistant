package com.kastack.auraassistant.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kastack.auraassistant.data.room.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY scheduledAt ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("""
        SELECT * FROM reminders 
        WHERE scheduledAt BETWEEN :startOfDay AND :endOfDay 
        ORDER BY scheduledAt ASC
    """)
    fun getTodayReminders(startOfDay: Long, endOfDay: Long): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Query("UPDATE reminders SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)

    @Query("SELECT * FROM reminders WHERE isSynced = 0")
    fun getUnsyncedReminders(): Flow<List<ReminderEntity>>

    @Query("UPDATE reminders SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
}