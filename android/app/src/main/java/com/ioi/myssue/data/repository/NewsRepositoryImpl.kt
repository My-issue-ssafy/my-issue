package com.ioi.myssue.data.repository

import com.ioi.myssue.data.dto.response.toDomain
import com.ioi.myssue.data.network.api.NewsApi
import com.ioi.myssue.domain.repository.NewsRepository
import javax.inject.Inject

class NewsRepositoryImpl @Inject constructor(
    private val newsApi: NewsApi
) : NewsRepository {

    override suspend fun getNews() = newsApi.getNews().map{ it.toDomain() }
}