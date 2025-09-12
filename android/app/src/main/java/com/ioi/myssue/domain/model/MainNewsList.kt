package com.ioi.myssue.domain.model

data class NewsMainList (
    val hot: List<News>,
    val recommend: List<News>,
    val recent: List<News>
)