package com.ioi.myssue.data.dto.response

import com.ioi.myssue.domain.model.CartoonNews
import kotlinx.serialization.Serializable

// 예시 응답 데이터 클래스 수정 필요함

@Serializable
data class ToonResponse(
    val id: Long,
    val title: String,
    val description: String,
    val newsContent: String,
    val toonImageUrl: String,
    val newsImageUrl: String,
    val likeCount: Int,
    val hateCount: Int,
    val createdAt: String,
)

fun ToonResponse.toDomain() = CartoonNews(
    newsTitle = title,
    newsDescription = description,
    newsFullContent = newsContent,
    newsImageUrl = newsImageUrl,
    toonImageUrl = toonImageUrl
)