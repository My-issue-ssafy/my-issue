// AnalyticsLogger.kt
package com.ioi.myssue.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics


interface AnalyticsLogger {
    fun setUserId(userId: String?)

    fun logScreenView(screenName: String, screenClass: String = screenName)

    fun logNewsImpression(newsId: Long, feedSource: String)

    fun logNewsClick(newsId: Long, feedSource: String)

    fun logNewsViewEnd(newsId: Long, dwellMs: Long, scrollPct: Int, fromSource: String)

    fun logNewsBookmark(newsId: Long, action: String) // "add" or "remove"
}