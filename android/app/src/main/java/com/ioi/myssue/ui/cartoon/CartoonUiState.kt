package com.ioi.myssue.ui.cartoon

import com.ioi.myssue.domain.model.CartoonNews

data class CartoonUiState(
    val cartoonNewsList: List<CartoonNews> = emptyList(),
    val currentCartoonIndex: Int = 0,
    val exitTrigger: Int = 0,
    val isSwiping: Boolean = false,
    val isLikePressed: Boolean = false,
    val isHatePressed: Boolean = false,

    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean
        get() = cartoonNewsList.isEmpty() || currentCartoonIndex == cartoonNewsList.size

    fun canInteract(): Boolean = currentCartoonIndex < cartoonNewsList.size && !isLikePressed && !isHatePressed
}