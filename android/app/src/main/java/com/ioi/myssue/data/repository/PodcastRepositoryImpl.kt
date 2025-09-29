package com.ioi.myssue.data.repository

import com.ioi.myssue.data.dto.response.toDomain
import com.ioi.myssue.data.network.api.PodcastApi
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.domain.model.PodcastEpisode
import com.ioi.myssue.domain.repository.PodcastRepository
import javax.inject.Inject

class PodcastRepositoryImpl @Inject constructor(
    private val podcastApi: PodcastApi
): PodcastRepository {

    override suspend fun getPodcast(date: String) =
        podcastApi.getPodcast(date).toDomain()

    override suspend fun getNewsByPodcast(podcastId: Long) =
        podcastApi.getNewsByPodcastId(podcastId).map{ it.toDomain() }
}