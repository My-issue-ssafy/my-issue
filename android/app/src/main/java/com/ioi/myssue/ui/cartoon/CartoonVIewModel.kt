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
                    newsTitle = "어른이? 얼음이?\n말장난 속 오해 발생",
                    newsDescription = "병아리가 '어른이'를 묻자\n닭이 설을 시작하지만,\n대화는 '얼음이'라는 말장난으로 이어져 웃음을 자아냈다.",
                    cartoonUrl = "android.resource://com.ioi.myssue/drawable/cartoon_example_1"
                ),
                CartoonNews(
                    newsTitle = "뉴스 제목 2",
                    newsDescription = "뉴스 설명 2",
                    cartoonUrl = "android.resource://com.ioi.myssue/drawable/cartoon_example_2"
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