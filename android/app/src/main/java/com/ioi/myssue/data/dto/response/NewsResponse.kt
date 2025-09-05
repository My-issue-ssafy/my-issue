package com.ioi.myssue.data.dto.response

import com.ioi.myssue.domain.model.News
import kotlinx.serialization.Serializable

@Serializable
data class NewsResponse(
    val title: String,
    val content: String,
    val url: String
)

fun NewsResponse.toDomain() = News(
    title = title,
    content = content,
    url = url
)