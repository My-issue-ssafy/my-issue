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
    override fun setUserId(userId: String?) {
        fa.setUserId(userId)
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

    /**
     * 뉴스 노출(피드에서 보여진 경우) 이벤트 로깅
     * @param newsId 뉴스 고유 ID
     * @param feedSource 노출된 피드 출처 (예: "home", "recommended")
     */
    override fun logNewsImpression(newsId: Long, feedSource: String) {
        val params = Bundle().apply {
            putLong("news_id", newsId)
            putString("feed_source", feedSource)
        }
        fa.logEvent("news_impression", params)
    }

    /**
     * 뉴스 클릭 이벤트 로깅
     * @param newsId 클릭된 뉴스 고유 ID
     * @param feedSource 클릭 발생한 피드 출처
     */
    override fun logNewsClick(newsId: Long, feedSource: String) {
        val params = Bundle().apply {
            putLong("news_id", newsId)
            putString("feed_source", feedSource)
        }
        fa.logEvent("news_click", params)
    }

    /**
     * 뉴스 상세 조회 종료 이벤트 로깅
     * @param newsId 뉴스 고유 ID
     * @param dwellMs 머문 시간 (ms 단위)
     * @param scrollPct 스크롤 퍼센트 (0~100)
     * @param fromSource 어떤 경로에서 진입했는지 (예: "push", "home_feed")
     */
    override fun logNewsViewEnd(newsId: Long, dwellMs: Long, scrollPct: Int, fromSource: String) {
        val params = Bundle().apply {
            putLong("news_id", newsId)
            putLong("dwell_ms", dwellMs)
            putInt("scroll_pct", scrollPct)
            putString("from_source", fromSource)
        }
        fa.logEvent("news_view_end", params)
    }

    /**
     * 뉴스 북마크 추가/제거 이벤트 로깅
     * @param newsId 뉴스 고유 ID
     * @param action 북마크 동작 ("add" = 추가, "remove" = 제거)
     */
    override fun logNewsBookmark(newsId: Long, action: String) {
        val params = Bundle().apply {
            putLong("news_id", newsId)
            putString("action", action)
        }
        fa.logEvent("news_bookmark", params)
    }

    private inline fun FirebaseAnalytics.logEvent(
        name: String,
        crossinline block: Bundle.() -> Unit
    ) {
        val bundle = Bundle().apply(block)
        logEvent(name, bundle)
    }
}
