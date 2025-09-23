package com.ioi.myssue.data.dto.response

import com.ioi.myssue.domain.model.Notification
import com.ioi.myssue.domain.model.NotificationPage
import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val notificationId: Int,
    val newsId: Int,
    val content: String,
    val thumbnail: String?,
    val createdAt: String,
    val read: Boolean
)

@Serializable
data class NotificationPageResponse(
    val content: List<NotificationResponse>,
    val hasNext: Boolean
)

fun NotificationResponse.toDomain(): Notification = Notification(
    notificationId = notificationId,
    newsId = newsId,
    content = content,
    thumbnail = thumbnail,
    createdAt = createdAt,
    read = read
)

fun NotificationPageResponse.toDomain(): NotificationPage = NotificationPage(
    content = content.map { it.toDomain() },
    hasNext = hasNext
)
