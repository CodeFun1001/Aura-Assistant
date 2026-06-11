package com.kastack.auraassistant.data.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kastack.auraassistant.domain.models.MessageMeta
import com.kastack.auraassistant.domain.models.Sender

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val age: String = "",
    val phone: String = "",
    val traits: String = "",
    val isOnboardingComplete: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis(),
    val meta: MessageMeta = MessageMeta(),
    val isSynced: Boolean = false
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val scheduledAt: Long,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)