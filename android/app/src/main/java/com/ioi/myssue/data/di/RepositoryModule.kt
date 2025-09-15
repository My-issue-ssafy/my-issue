package com.ioi.myssue.data.di

import com.ioi.myssue.data.repository.fake.FakeCartoonRepositoryImpl
import com.ioi.myssue.data.repository.FakeNewsRepositoryImpl
import com.ioi.myssue.domain.repository.AuthRepository
import com.ioi.myssue.domain.repository.CartoonRepository
import com.ioi.myssue.domain.repository.NewsRepository
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
    fun bindNewsRepository(repository: FakeNewsRepositoryImpl): NewsRepository

    @Singleton
    @Binds
    fun bindCartoonRepository(repository: FakeCartoonRepositoryImpl): CartoonRepository

}