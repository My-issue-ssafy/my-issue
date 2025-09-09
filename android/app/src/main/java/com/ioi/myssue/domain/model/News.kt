package com.ioi.myssue.domain.model

data class News(
    val title: String,
    val content: String,
    val url: String,
    val img: String,
    val category: String,
    val createdAt: String,
    val views: Int
)
