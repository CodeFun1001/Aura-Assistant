package com.kastack.auraassistant.di

import com.kastack.auraassistant.data.room.AuraDatabase
import com.kastack.auraassistant.data.room.dao.ChatMessageDao
import com.kastack.auraassistant.data.room.dao.ReminderDao
import com.kastack.auraassistant.data.room.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideChatMessageDao(db: AuraDatabase): ChatMessageDao = db.chatMessageDao()

    @Provides
    @Singleton
    fun provideReminderDao(db: AuraDatabase): ReminderDao = db.reminderDao()

    @Provides
    @Singleton
    fun provideUserProfileDao(db: AuraDatabase): UserProfileDao = db.userProfileDao()
}