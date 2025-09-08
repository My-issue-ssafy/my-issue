package com.ioi.myssue.ui.cartoon
data class CartoonNews(
    val newsTitle: String = "",
    val newsDescription: String = "",
    val newsFullContent: String = "",
    val newsUrl: String = "",
    val cartoonUrl: String = ""
)

data class CartoonUiState(
    val cartoonNewsList: List<CartoonNews> = emptyList(),
    val currentCartoonIndex: Int = 0,
    val exitTrigger: Int = 0,
    val isLikePressed: Boolean = false,
    val isHatePressed: Boolean = false,

    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean
        get() = cartoonNewsList.isEmpty() || currentCartoonIndex == cartoonNewsList.size
    fun canInteract(): Boolean = currentCartoonIndex < cartoonNewsList.size && !isLikePressed && !isHatePressed
    fun getCurrentCartoon(): CartoonNews? = cartoonNewsList.getOrNull(currentCartoonIndex)
}