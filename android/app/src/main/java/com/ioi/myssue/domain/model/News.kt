package com.ioi.myssue.domain.model

data class News(
    val title: String,
    val content: List<NewsBlock> = emptyList(),
    val url: String,
    val img: String,
    val category: String,
    val createdAt: String,
    val relativeTime: String,
    val views: Int,
    val author: String,
    val newspaper: String,
)
