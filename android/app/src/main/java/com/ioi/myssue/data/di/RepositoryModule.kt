package com.ioi.myssue.data.di

import com.ioi.myssue.data.repository.CartoonRepositoryImpl
import com.ioi.myssue.data.repository.ChatBotRepositoryImpl
import com.ioi.myssue.data.repository.NewsRepositoryImpl
import com.ioi.myssue.data.repository.NotificationRepositoryImpl
import com.ioi.myssue.data.repository.PodcastRepositoryImpl
import com.ioi.myssue.domain.repository.CartoonRepository
import com.ioi.myssue.domain.repository.ChatBotRepository
import com.ioi.myssue.domain.repository.NewsRepository
import com.ioi.myssue.domain.repository.NotificationRepository
import com.ioi.myssue.domain.repository.PodcastRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Singleton
    @Binds
    fun bindNewsRepository(repository: NewsRepositoryImpl): NewsRepository

    @Singleton
    @Binds
    fun bindCartoonRepository(repository: CartoonRepositoryImpl): CartoonRepository

    @Singleton
    @Binds
    fun bindChatBotRepository(repository: ChatBotRepositoryImpl) : ChatBotRepository

    @Singleton
    @Binds
    fun bindPodcastRepository(repository: PodcastRepositoryImpl) : PodcastRepository

    @Singleton
    @Binds
    fun bindNotificationRepository(repository: NotificationRepositoryImpl): NotificationRepository
}