package com.ioi.myssue.domain.repository

import com.ioi.myssue.domain.model.News

interface NewsRepository {

    suspend fun getNews(): List<News>
}