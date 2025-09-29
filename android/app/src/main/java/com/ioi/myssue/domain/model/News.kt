package com.ioi.myssue.domain.model

data class News(
    val newsId: Long,
    val title: String,
    val author: String,
    val newspaper: String,
    val createdAt: String,
    val views: Int,
    val category: String,
    val thumbnail: String?,
    val content: List<NewsBlock> = emptyList(),
    val displayTime: String,
    val scrapCount: Int,
    val isScraped: Boolean
)
