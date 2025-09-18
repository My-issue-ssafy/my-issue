package com.ioi.myssue.ui.mypage.mytoon

import com.ioi.myssue.domain.model.CartoonNews

data class MyCartoonUiState(
    val myToons: List<CartoonNews> = emptyList(),
    val clickedToon: CartoonNews? = null,
    val selectedNewsId: Long? = null,
)