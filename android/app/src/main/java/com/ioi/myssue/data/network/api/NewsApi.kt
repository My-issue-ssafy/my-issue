package com.ioi.myssue.data.network.api

import com.ioi.myssue.data.dto.response.NewsResponse
import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface NewsApi {

    @GET
    suspend fun getNews(): List<NewsResponse>
}

