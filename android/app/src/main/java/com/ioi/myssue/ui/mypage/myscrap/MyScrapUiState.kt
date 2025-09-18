package com.ioi.myssue.ui.mypage.myscrap

import com.ioi.myssue.domain.model.CartoonNews
import com.ioi.myssue.domain.model.NewsSummary

data class MyScrapUiState(
    val isNotificationEnabled: Boolean = true,
    val newsSummaries: List<NewsSummary> = emptyList(),
    val myToons: List<CartoonNews> = emptyList(),
    val selectedNewsId: Long? = null,
    val cursor: String? = null,
    val hasNext: Boolean = true,
    val isLoading: Boolean = false
)