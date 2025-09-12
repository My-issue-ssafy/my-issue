package com.ioi.myssue.ui.news

import com.ioi.myssue.domain.model.NewsBlock

data class NewsDetailUiState(
    val newsId: Int? = null,
    val title: String = "",
    val author: String = "",
    val newspaper: String = "",
    val createdAt: String = "",
    val blocks: List<NewsBlock> = emptyList(),
    val isBookmarked: Boolean = false,

    val loading: Boolean = true,
    val error: String? = null,
)