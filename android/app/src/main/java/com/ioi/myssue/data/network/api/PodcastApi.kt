package com.ioi.myssue.data.network.api

import com.ioi.myssue.data.dto.response.PodcastNewsSummaryResponse
import com.ioi.myssue.data.dto.response.PodcastResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PodcastApi {

    @GET("podcast")
    suspend fun getPodcast(
        @Query("date") date: String
    ): PodcastResponse

    @GET("podcast/{podcastId}/news")
    suspend fun getNewsByPodcastId(
        @Path("podcastId") podcastId : Long
    ): List<PodcastNewsSummaryResponse>
}

