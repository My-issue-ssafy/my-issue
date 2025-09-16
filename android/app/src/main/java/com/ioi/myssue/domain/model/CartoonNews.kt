package com.ioi.myssue.domain.model

data class CartoonNews(
    val toonId: Long = -1L,
    val newsId: Long = -1L,
    val newsTitle: String = "",
    val newsDescription: String = "",
    val toonImageUrl: String = ""
)