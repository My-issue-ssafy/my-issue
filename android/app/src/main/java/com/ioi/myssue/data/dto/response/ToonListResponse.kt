package com.ioi.myssue.data.dto.response

import com.ioi.myssue.domain.model.CartoonNews
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 예시 응답 데이터 클래스 수정 필요함

@Serializable
data class ToonResponse(
    val toonId: Long,
    val newsId: Long,
//    val title: String,
    @SerialName("summary") val description: String,
//    val newsContent: String,
    @SerialName("toonImage") val toonImageUrl: String,
//    val newsImageUrl: String,
)

fun ToonResponse.toDomain() = CartoonNews(
    toonId = toonId ,
    newsId = newsId,
    newsTitle = "title",
    newsDescription = description,
    newsFullContent = "newsContent",
    newsImageUrl = "newsImageUrl",
    toonImageUrl = toonImageUrl
)