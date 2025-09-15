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

    // 1. 툰 노출 (toon_impression)
    override fun logToonImpression(newsId: Long) {
        val params = Bundle().apply {
            putLong("news_id", newsId)
        }
        fa.logEvent("toon_impression", params)
    }


    // 2. 툰 터치 → 관련 뉴스 요약 보기 (toon_click)
    override fun logToonClick(newsId: Long) {
        val params = Bundle().apply {
            putLong("news_id", newsId)
        }
        fa.logEvent("toon_click", params)
    }

    // 3. 툰 오른쪽 슬라이드 → 관심 있음/좋다 (toon_positive)
    override fun logToonPositive(newsId: Long) {
        val params = Bundle().apply {
            putLong("news_id", newsId)
        }
        fa.logEvent("toon_positive", params)
    }

    // 4. 툰 왼쪽 슬라이드 → 관심 없음/싫다 (toon_negative)
    override fun logToonNegative(newsId: Long) {
        val params = Bundle().apply {
            putLong("news_id", newsId)
        }
        fa.logEvent("toon_negative", params)
    }

    // 5. 툰 위로 슬라이드 → 관련 뉴스 전체보기 (toon_expand_news)
    override fun logToonExpandNews(newsId: Long) {
        val params = Bundle().apply {
            putLong("news_id", newsId)
        }
        fa.logEvent("toon_expand_news", params)
    }

    private inline fun FirebaseAnalytics.logEvent(
        name: String,
        crossinline block: Bundle.() -> Unit
    ) {
        val bundle = Bundle().apply(block)
        logEvent(name, bundle)
    }
}
