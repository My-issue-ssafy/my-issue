package com.ioi.myssue.data.di

import com.ioi.myssue.data.repository.CartoonRepositoryImpl
import com.ioi.myssue.data.repository.NewsRepositoryImpl
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
    fun bindNewsRepository(repository: NewsRepositoryImpl): NewsRepository

    @Singleton
    @Binds
    fun bindCartoonRepository(repository: CartoonRepositoryImpl): CartoonRepository

}