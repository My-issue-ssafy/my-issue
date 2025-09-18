package com.ioi.myssue.domain.repository

import com.ioi.myssue.domain.model.CursorPage
import com.ioi.myssue.domain.model.MainNewsList
import com.ioi.myssue.domain.model.News
import com.ioi.myssue.domain.model.NewsPage
import com.ioi.myssue.domain.model.NewsSummary

interface NewsRepository {
    suspend fun getNews(keyword: String? = null, category: String? = null, size: Int, lastId : Long?): NewsPage

    suspend fun getMainNews(): MainNewsList

    suspend fun getHotNews(cursor: String? = null, size: Int = 20): CursorPage<NewsSummary>

    suspend fun getRecommendNews(cursor: String? = null, size: Int = 20): CursorPage<NewsSummary>

    suspend fun getTrendNews(cursor: String? = null, size: Int = 20): CursorPage<NewsSummary>

    suspend fun getNewsDetail(newsId: Long): News

    suspend fun bookMarkNews(newsId: Long): Boolean

    suspend fun getBookmarkedNews(cursor: String? = null, size: Int = 10): CursorPage<NewsSummary>
}