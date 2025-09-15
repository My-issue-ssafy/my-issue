package com.ioi.myssue.data.di

import com.ioi.myssue.data.network.api.AuthApi
import com.ioi.myssue.data.network.api.CartoonApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton
import kotlin.jvm.java


@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideAuthApi(
        @Named("auth") retrofit: Retrofit,
    ): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideToonApi(
        @Named("main") retrofit: Retrofit,
    ): CartoonApi = retrofit.create(CartoonApi::class.java)
}