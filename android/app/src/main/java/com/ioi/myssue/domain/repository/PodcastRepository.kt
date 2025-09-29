package com.ioi.myssue.domain.repository

import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.domain.model.PodcastEpisode

interface PodcastRepository {

    suspend fun getPodcast(date: String): PodcastEpisode

    suspend fun getNewsByPodcast(podcastId: Long): List<NewsSummary>
}