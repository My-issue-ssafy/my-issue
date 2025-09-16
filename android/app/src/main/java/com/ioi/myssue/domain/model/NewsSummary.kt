package com.ioi.myssue.domain.model

data class NewsSummary (
    val newsId: Long,
    val title: String,
    val author: String,
    val newspaper: String,
    val createdAt: String,
    val views: Int,
    val category: String,
    val thumbnail: String?,
    val relativeTime: String,
)