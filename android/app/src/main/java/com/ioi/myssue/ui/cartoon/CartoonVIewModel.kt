package com.ioi.myssue.ui.cartoon

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.math.abs


class CartoonViewModel @Inject constructor() : ViewModel() {

    private var _state = MutableStateFlow(CartoonUiState())
    val state = _state.asStateFlow()

    init {
        loadCartoon()
    }

    private fun loadCartoon() {
        _state.value = _state.value.copy(
            cartoonNewsList = listOf(
                CartoonNews(
                    newsTitle = "뉴스 제목 1",
                    newsDescription = "뉴스 설명 1",
                    cartoonUrl = "android.resource://com.ioi.myssue/drawable/news_cartoon_example1"
                ),
                CartoonNews(
                    newsTitle = "뉴스 제목 2",
                    newsDescription = "뉴스 설명 2",
                    cartoonUrl = "android.resource://com.ioi.myssue/drawable/news_cartoon_example2"
                )
            ),
        )
    }

    fun onLikePressed() {
        if (!_state.value.canInteract()) return

        _state.value = _state.value.copy(
            isLikePressed = true,
            exitTrigger = abs(_state.value.exitTrigger) + 1
        )
    }

    fun onHatePressed() {
        if (!_state.value.canInteract()) return

        _state.value = _state.value.copy(
            isHatePressed = true,
            exitTrigger = (abs(_state.value.exitTrigger) + 1) * -1
        )
    }

    fun onExitFinished() {
        _state.value = _state.value.copy(
            currentCartoonIndex = _state.value.currentCartoonIndex + 1,
            isLikePressed = false,
            isHatePressed = false
        )
    }

    fun resetButtonStates() {
        _state.value = _state.value.copy(
            isLikePressed = false,
            isHatePressed = false
        )
    }
}