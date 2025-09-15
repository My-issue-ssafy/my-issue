package com.ioi.myssue.domain.repository

import com.ioi.myssue.domain.model.CursorPage
import com.ioi.myssue.domain.model.MainNewsList
import com.ioi.myssue.domain.model.News
import com.ioi.myssue.domain.model.NewsSummary

interface NewsRepository {
    suspend fun getMainNews(userId: Int? = null): MainNewsList

    suspend fun getHotNews(cursor: String? = null, size: Int = 20): CursorPage<NewsSummary>

    suspend fun getRecommendNews(cursor: String? = null, size: Int = 20): CursorPage<NewsSummary>

    suspend fun getTrendNews(cursor: String? = null, size: Int = 20): CursorPage<NewsSummary>

    suspend fun getNewsDetail(newsId: Long): News

    suspend fun isBookmarked(newsId: Long): Boolean
    suspend fun setBookmarked(newsId: Long, target: Boolean): Boolean
}