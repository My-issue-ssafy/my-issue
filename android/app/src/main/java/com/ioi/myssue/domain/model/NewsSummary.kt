package com.ioi.myssue.domain.model

data class NewsSummary (
    val newsId: Long = -1L,
    val title: String = "",
    val author: String = "",
    val newspaper: String = "",
    val createdAt: String = "",
    val views: Int = 0,
    val category: String = "",
    val thumbnail: String? = null,
    val relativeTime: String = "",
)