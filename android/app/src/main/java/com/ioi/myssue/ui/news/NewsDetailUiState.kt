package com.ioi.myssue.ui.news

import com.ioi.myssue.domain.model.NewsBlock

data class NewsDetailUiState(
    val newsId: Long = -1L,
    val title: String = "",
    val author: String = "",
    val newspaper: String = "",
    val displayTime: String = "",
    val thumbnail: String? = null,
    val blocks: List<NewsBlock> = emptyList(),
    val isOpen: Boolean = false,
    val isBookmarked: Boolean = false,
    val scrapCount: Int = 0,
    val c: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
)