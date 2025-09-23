package com.ioi.myssue.domain.model

data class Notification(
    val notificationId: Int,
    val newsId: Int,
    val content: String,
    val thumbnail: String?,
    val createdAt: String,
    val read: Boolean
)

data class NotificationPage(
    val content: List<Notification>,
    val hasNext: Boolean
)