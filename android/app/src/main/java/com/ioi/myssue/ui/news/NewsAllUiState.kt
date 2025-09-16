package com.ioi.myssue.ui.news

import com.ioi.myssue.domain.model.NewsSummary

data class NewsAllUiState(
    val items: List<NewsSummary> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false,

    val selectedId: Long? = null,
    val isInitialLoading: Boolean = false
)