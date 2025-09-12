package com.ioi.myssue.domain.model

data class MainNewsList (
    val hot: List<News> = emptyList(),
    val recommend: List<News> = emptyList(),
    val recent: List<News> = emptyList()
)