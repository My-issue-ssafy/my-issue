package com.ioi.myssue.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.ioi.myssue.authDataStore
import com.ioi.myssue.data.datastore.AuthDataStore
import com.ioi.myssue.data.network.AuthManager
import com.ioi.myssue.data.network.TokenAuthenticator
import com.ioi.myssue.data.network.api.AuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Authenticator
import javax.inject.Named
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthManger(
        authDataStore: AuthDataStore,
        authApi: AuthApi
    ): AuthManager =
        AuthManager(authDataStore, authApi, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    @Provides
    @Singleton
    fun provideAuthDataStore(
        @Named("auth_preferences") dataStore: DataStore<Preferences>
    ): AuthDataStore = AuthDataStore(dataStore)

    @Provides
    @Singleton
    @Named("auth_preferences")
    fun provideSearchHistoryPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return context.authDataStore
    }

    @Provides
    @Singleton
    fun provideAuthenticator(
        authManager: AuthManager,
        authApi: AuthApi
    ): Authenticator = TokenAuthenticator(authManager, authApi)
}
