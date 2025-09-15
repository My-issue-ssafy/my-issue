package com.ioi.myssue.data.dto.response

import com.ioi.myssue.common.util.TimeConverter
import com.ioi.myssue.domain.model.News
import com.ioi.myssue.domain.model.NewsBlock
import kotlinx.serialization.Serializable


@Serializable
data class NewsContentResponse(
    val type: String,
    val content: String
)

@Serializable
data class NewsCardResponse(
    val newsId : Long,
    val title: String,
    val content: List<NewsContentResponse>,
    val category: String,
    val author: String,
    val newspaper: String,
    val createdAt: String,
    val relativeTime: String,
    val views: Int,
    val url: String,
    val img: String?,
)

fun NewsCardResponse.toDomain(time: TimeConverter) = News(
    newsId = newsId,
    title = title,
    content = content.toDomainBlocks(),
    category = category,
    author = author,
    newspaper = newspaper,
    createdAt = time.toDisplay(createdAt),
    relativeTime = time.toRelative(createdAt),
    views = views,
    url = url,
    img = img,
)

private fun List<NewsContentResponse>.toDomainBlocks(): List<NewsBlock> =
    mapNotNull { dto ->
        when (dto.type.lowercase()) {
            "image" -> NewsBlock.Image(dto.content)
            "desc"  -> NewsBlock.Desc(dto.content)
            "text"  -> NewsBlock.Text(dto.content)
            else    -> null
        }
    }