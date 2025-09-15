package com.ioi.myssue.domain.repository

import com.ioi.myssue.domain.model.MainNewsList
import com.ioi.myssue.domain.model.News
import com.ioi.myssue.domain.model.NewsPage

interface NewsRepository {
    suspend fun getMainNews(): MainNewsList
    suspend fun getHotNews(cursor: String? = null, pageSize: Int = 20): NewsPage
    suspend fun getRecommendNews(cursor: String? = null, pageSize: Int = 20): NewsPage
    suspend fun getRecentNews(cursor: String? = null, pageSize: Int = 20): NewsPage
    suspend fun getNewsById(newsId: Long): News
    suspend fun isBookmarked(newsId: Long): Boolean
    suspend fun setBookmarked(newsId: Long, target: Boolean): Boolean
}