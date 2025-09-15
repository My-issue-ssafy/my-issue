package com.ioi.myssue.domain.model

data class News(
    val newsId: Long,
    val title: String,
    val content: List<NewsBlock> = emptyList(),
    val url: String,
    val category: String,
    val newspaper: String,
    val createdAt: String,
    val relativeTime: String,
    val views: Int,
    val img: String?,
    val author: String,
)
