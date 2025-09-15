package com.ioi.myssue.data.di

import android.content.Context
import com.ioi.myssue.BuildConfig
import com.ioi.myssue.authDataStore
import com.ioi.myssue.data.network.AuthInterceptor
import com.ioi.myssue.data.network.DataStoreCookieJar
import com.ioi.myssue.data.network.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.CookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideCookieJar(@ApplicationContext context: Context): CookieJar =
        DataStoreCookieJar(context.authDataStore)

    @Provides
    @Singleton
    @Named("main")
    fun provideMainHttpClient(
        cookieJar: CookieJar,
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client =
            OkHttpClient.Builder()
                .readTimeout(100, TimeUnit.SECONDS)
                .connectTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(100, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
                .authenticator(authenticator)
        return client.build()
    }

    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthHttpClient(
        cookieJar: CookieJar
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client =
            OkHttpClient.Builder()
                .readTimeout(100, TimeUnit.SECONDS)
                .connectTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(100, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .addInterceptor(logging)
        return client.build()
    }

    @Provides
    @Singleton
    @Named("main")
    fun provideMainRetrofit(
        @Named("main") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(Json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .build()
    }
    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthRetrofit(
        @Named("auth") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(Json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .build()
    }
}