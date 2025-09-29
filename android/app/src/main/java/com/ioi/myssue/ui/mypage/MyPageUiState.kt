package com.ioi.myssue.ui.mypage

import com.ioi.myssue.domain.model.CartoonNews
import com.ioi.myssue.domain.model.NewsSummary

data class MyPageUiState(
    val isNotificationEnabled: Boolean = true,
    val newsSummaries: List<NewsSummary> = emptyList(),
    val myToons: List<CartoonNews> = emptyList(),
    val selectedNewsId: Long? = null,
    val isLoadingScrapNews: Boolean = false,
    val isLoadingLikeToons: Boolean = false
)