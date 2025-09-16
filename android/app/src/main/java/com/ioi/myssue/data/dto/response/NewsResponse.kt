package com.ioi.myssue.data.dto.response

import com.ioi.myssue.common.util.TimeConverter
import com.ioi.myssue.domain.model.CursorPage
import com.ioi.myssue.domain.model.NewsSummary
import kotlinx.serialization.Serializable

// 뉴스 카드 목록
@Serializable
data class NewsCardResponse(
    val newsId: Long,
    val title: String,
    val author: String? = null,
    val newspaper: String? = null,
    val createdAt: String? = null,
    val views: Int,
    val category: String,
    val thumbnail: String? = null
)

// 뉴스 섹션 전체보기 페이지
@Serializable
data class CursorPageNewsResponse(
    val items: List<NewsCardResponse>,
    val nextCursor: String? = null,
    val hasNext: Boolean
)

// 뉴스 메인 목록
@Serializable
data class NewsMainResponse(
    val hot: List<NewsCardResponse>,
    val recommend: List<NewsCardResponse>,
    val latest: List<NewsCardResponse>
)

fun NewsCardResponse.toDomain(time: TimeConverter) = NewsSummary(
    newsId = newsId,
    title = title,
    author = author ?: "-",
    newspaper = newspaper ?: "-",
    createdAt = createdAt ?: "-",
    views = views,
    category = category,
    thumbnail = thumbnail,
    relativeTime = time.toRelative(createdAt ?: "-"),
)

fun CursorPageNewsResponse.toDomain(time: TimeConverter) = CursorPage(
    items = items.map { it.toDomain(time) },
    nextCursor = nextCursor,
    hasNext = hasNext
)