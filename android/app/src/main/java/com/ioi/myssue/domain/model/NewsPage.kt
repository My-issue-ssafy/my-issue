package com.ioi.myssue.domain.model

data class NewsPage(
    val newsItems: List<NewsSummary>,
    val lastId: Long?,
    val hasNext: Boolean
)
