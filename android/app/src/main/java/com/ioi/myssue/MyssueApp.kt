package com.ioi.myssue

import android.app.Application
import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.ioi.myssue.analytics.AnalyticsLogger
import com.ioi.myssue.data.network.AuthManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

val LocalAnalytics = staticCompositionLocalOf<AnalyticsLogger> {
    error("AnalyticsLogger not provided")
}

val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

@HiltAndroidApp
class MyssueApp: Application() {


}