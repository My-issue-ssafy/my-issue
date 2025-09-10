package com.ioi.myssue

import android.app.Application
import androidx.compose.runtime.staticCompositionLocalOf
import com.ioi.myssue.analytics.AnalyticsLogger
import dagger.hilt.android.HiltAndroidApp

val LocalAnalytics = staticCompositionLocalOf<AnalyticsLogger> {
    error("AnalyticsLogger not provided")
}

@HiltAndroidApp
class MyssueApp: Application() {


}