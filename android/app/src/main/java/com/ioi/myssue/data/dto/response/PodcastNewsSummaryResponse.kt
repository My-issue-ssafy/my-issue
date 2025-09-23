package com.ioi.myssue.data.dto.response

import com.ioi.myssue.domain.model.NewsSummary


data class PodcastNewsSummaryResponse(
    val newsId: Long,
    val thumbnailUrl: String,
    val title: String,
    val category: String
)

fun PodcastNewsSummaryResponse.toDomain() = NewsSummary(
    newsId = newsId,
    title = title,
    thumbnail = thumbnailUrl,
    category = category,
)