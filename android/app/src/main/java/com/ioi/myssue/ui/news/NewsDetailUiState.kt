package com.ioi.myssue.ui.news

import com.ioi.myssue.domain.model.NewsBlock

data class NewsDetailUiState(
    val newsId: Long? = null,
    val title: String = "",
    val author: String = "",
    val newspaper: String = "",
    val displayTime: String = "",
    val blocks: List<NewsBlock> = emptyList(),
    val isOpen: Boolean = false,
    val isBookmarked: Boolean = false,

    val loading: Boolean = true,
    val error: String? = null,
)