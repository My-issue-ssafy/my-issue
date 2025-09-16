package com.ioi.myssue.domain.model

data class MainNewsList (
    val hot: List<NewsSummary> = emptyList(),
    val recommend: List<NewsSummary> = emptyList(),
    val latest: List<NewsSummary> = emptyList()
)