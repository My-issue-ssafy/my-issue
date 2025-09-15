package com.ioi.myssue.domain.model

data class NewsPage(
    val items: List<News> = emptyList(),
    val nextCursor: String? = null
)