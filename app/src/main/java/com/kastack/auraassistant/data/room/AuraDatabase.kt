package com.kastack.auraassistant.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kastack.auraassistant.data.room.converters.MessageMetaConverter
import com.kastack.auraassistant.data.room.converters.SenderConverter
import com.kastack.auraassistant.data.room.dao.ChatMessageDao
import com.kastack.auraassistant.data.room.dao.ReminderDao
import com.kastack.auraassistant.data.room.dao.UserProfileDao
import com.kastack.auraassistant.data.room.entities.ChatMessageEntity
import com.kastack.auraassistant.data.room.entities.ReminderEntity
import com.kastack.auraassistant.data.room.entities.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        ChatMessageEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(MessageMetaConverter::class, SenderConverter::class)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun reminderDao(): ReminderDao
}