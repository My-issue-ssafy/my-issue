package com.ioi.myssue.domain.repository

import com.ioi.myssue.domain.model.News

interface NewsRepository {
    suspend fun getHotNews(): List<News>
    suspend fun getRecommendedNews(): List<News>
    suspend fun getRecentNews(): List<News>
}