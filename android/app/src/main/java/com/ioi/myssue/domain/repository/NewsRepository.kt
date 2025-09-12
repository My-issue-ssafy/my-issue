package com.ioi.myssue.domain.repository

import com.ioi.myssue.domain.model.MainNewsList
import com.ioi.myssue.domain.model.News
import com.ioi.myssue.domain.model.NewsPage

interface NewsRepository {
    suspend fun getMainNews(): MainNewsList
    suspend fun getHotNews(cursor: String? = null, pageSize: Int = 20): NewsPage
    suspend fun getRecommendNews(cursor: String? = null, pageSize: Int = 20): NewsPage
    suspend fun getRecentNews(cursor: String? = null, pageSize: Int = 20): NewsPage
    suspend fun getNewsById(id: Int): News
    suspend fun isBookmarked(newsId: Int): Boolean
    suspend fun setBookmarked(newsId: Int, target: Boolean): Boolean
}