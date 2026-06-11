package com.kastack.auraassistant.di

import com.kastack.auraassistant.data.repository.ChatRepositoryImpl
import com.kastack.auraassistant.data.repository.ReminderRepositoryImpl
import com.kastack.auraassistant.data.repository.UserProfileRepositoryImpl
import com.kastack.auraassistant.domain.repositories.ChatRepository
import com.kastack.auraassistant.domain.repositories.ReminderRepository
import com.kastack.auraassistant.domain.repositories.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        impl: UserProfileRepositoryImpl
    ): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(
        impl: ReminderRepositoryImpl
    ): ReminderRepository
}
