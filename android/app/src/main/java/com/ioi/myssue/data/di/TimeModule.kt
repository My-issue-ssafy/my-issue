package com.ioi.myssue.data.di

import com.ioi.myssue.common.util.TimeConverter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideZoneId(): ZoneId = ZoneId.of("Asia/Seoul")

    @Provides
    @Singleton
    fun provideNewsTimeConverter(zoneId: ZoneId): TimeConverter =
        TimeConverter(zoneId)
}