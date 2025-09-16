package com.ioi.myssue.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAnalyticsLogger(
    private val fa: FirebaseAnalytics
) : AnalyticsLogger {

    /**
     * 현재 사용자 ID를 설정
     * - 로그인 시점에 호출
     */
    override fun setUserId(userId: Long) {
        fa.setUserId(userId.toString())
    }

    /**
     * 화면 진입 이벤트 로깅
     * @param screenName 화면 이름
     * @param screenClass 화면 클래스 이름 (기본적으로 screenName과 동일하게 사용 가능)
     */
    override fun logScreenView(screenName: String, screenClass: String) {
        fa.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
    }
    override fun logNewsImpression(newsId: Long, feedSource: String) {
        fa.logEvent("news_impression") {
            putLong("news_id", newsId)
            putString("feed_source", feedSource)
        }
    }

    override fun logNewsClick(newsId: Long, feedSource: String) {
        fa.logEvent("news_click") {
            putLong("news_id", newsId)
            putString("feed_source", feedSource)
        }
    }

    override fun logNewsViewEnd(newsId: Long, dwellMs: Long, scrollPct: Int, fromSource: String) {
        fa.logEvent("news_view_end") {
            putLong("news_id", newsId)
            putLong("dwell_ms", dwellMs)
            putInt("scroll_pct", scrollPct)
            putString("from_source", fromSource)
        }
    }

    override fun logNewsBookmark(newsId: Long, action: String) {
        fa.logEvent("news_bookmark") {
            putLong("news_id", newsId)
            putString("action", action)
        }
    }

    // 툰 관련 이벤트
    override fun logToonImpression(newsId: Long) {
        fa.logEvent("toon_impression") {
            putLong("news_id", newsId)
        }
    }

    override fun logToonClick(newsId: Long) {
        fa.logEvent("toon_click") {
            putLong("news_id", newsId)
        }
    }

    override fun logToonPositive(newsId: Long) {
        fa.logEvent("toon_positive") {
            putLong("news_id", newsId)
        }
    }

    override fun logToonNegative(newsId: Long) {
        fa.logEvent("toon_negative") {
            putLong("news_id", newsId)
        }
    }

    override fun logToonExpandNews(newsId: Long) {
        fa.logEvent("toon_expand_news") {
            putLong("news_id", newsId)
        }
    }

    private inline fun FirebaseAnalytics.logEvent(
        name: String,
        crossinline block: Bundle.() -> Unit
    ) {
        val bundle = Bundle().apply(block)
        logEvent(name, bundle)
    }
}
