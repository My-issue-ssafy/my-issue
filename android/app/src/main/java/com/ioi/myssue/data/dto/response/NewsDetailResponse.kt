package com.ioi.myssue.data.dto.response

import com.ioi.myssue.common.util.TimeConverter
import com.ioi.myssue.domain.model.News
import com.ioi.myssue.domain.model.NewsBlock
import kotlinx.serialization.Serializable


// 뉴스 상세보기
@Serializable
data class NewsDetailResponse(
    val newsId: Long,
    val title: String,
    val content: List<NewsContentResponse>,
    val category: String,
    val author: String? = null,
    val newspaper: String? = null,
    val createdAt: String? = null,
    val views: Int,
    val scrapCount: Int,
    val isScraped: Boolean = false
)

// 뉴스 본문 블록
@Serializable
data class NewsContentResponse(
    val type: String,   // "text" | "image" | "img_desc"
    val content: String
)

fun NewsDetailResponse.toDomain(time: TimeConverter) = News(
    newsId = newsId,
    title = title,
    author = author ?: "-",
    newspaper = newspaper ?: "-",
    createdAt = createdAt ?: "-",
    views = views,
    category = category,
    thumbnail = getThumbnail(content),
    content = content.toDomainBlocks(),
    displayTime = time.toDisplay(createdAt ?: "-"),
    scrapCount = scrapCount,
    isScraped = isScraped
)


// 블록 매핑
private fun List<NewsContentResponse>.toDomainBlocks(): List<NewsBlock> =
    mapNotNull { dto ->
        when (dto.type.lowercase()) {
            "image" -> NewsBlock.Image(dto.content)
            "img_desc" -> NewsBlock.Desc(dto.content)
            "text" -> NewsBlock.Text(dto.content)
            else -> null
        }
    }

// 썸네일 이미지 추출
private fun getThumbnail(blocks: List<NewsContentResponse>): String? =
    blocks.firstOrNull { it.type.equals("image", ignoreCase = true) }?.content