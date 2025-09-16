package com.ioi.myssue.ui.news

import com.ioi.myssue.domain.model.MainNewsList

data class NewsMainUiState(
    val main: MainNewsList = MainNewsList(),
    val isInitialLoading: Boolean = false
)