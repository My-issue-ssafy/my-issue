package com.ioi.myssue.data.dto.response

import com.ioi.myssue.common.util.TimeConverter
import com.ioi.myssue.domain.model.News
import com.ioi.myssue.domain.model.NewsBlock
import kotlinx.serialization.Serializable


@Serializable
data class NewsContentDto(
    val type: String,
    val content: String
)

@Serializable
data class NewsResponse(
    val id : Int,
    val title: String,
    val content: List<NewsContentDto>,
    val url: String,
    val img: String,
    val category: String,
    val createdAt: String,
    val relativeTime: String,
    val views: Int,
    val author: String,
    val newspaper: String,
)

fun NewsResponse.toDomain(time: TimeConverter) = News(
    id = id,
    title = title,
    content = content.toDomainBlocks(),
    url = url,
    img = img,
    category = category,
    createdAt = time.toDisplay(createdAt),
    relativeTime = time.toRelative(createdAt),
    views = views,
    author = author,
    newspaper = newspaper,
)

private fun List<NewsContentDto>.toDomainBlocks(): List<NewsBlock> =
    mapNotNull { dto ->
        when (dto.type.lowercase()) {
            "image" -> NewsBlock.Image(dto.content)
            "desc"  -> NewsBlock.Desc(dto.content)
            "text"  -> NewsBlock.Text(dto.content)
            else    -> null
        }
    }