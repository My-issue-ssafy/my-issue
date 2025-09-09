package com.ioi.myssue.data.dto.response

import com.ioi.myssue.domain.model.News
import kotlinx.serialization.Serializable

@Serializable
data class NewsResponse(
    val title: String,
    val content: String,
    val url: String,
    val img: String,
    val category: String,
    val createdAt: String,
    val views: Int
)

fun NewsResponse.toDomain() = News(
    title = title,
    content = content,
    url = url,
    img = img,
    category = category,
    createdAt = createdAt,
    views = views
)