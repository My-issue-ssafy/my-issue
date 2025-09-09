package com.ioi.myssue.ui.cartoon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.domain.repository.CartoonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class CartoonViewModel @Inject constructor(
    private val cartoonRepository: CartoonRepository
) : ViewModel() {

    private var _state = MutableStateFlow(CartoonUiState())
    val state = _state.asStateFlow()

    init {
        loadCartoon()
    }

    private fun loadCartoon() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)

        runCatching { cartoonRepository.getCartoonNews() }
            .onSuccess { cartoonNews ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    cartoonNewsList = cartoonNews
                )
            }
    }

    fun onLikePressed(isSwiping: Boolean = false) {
        if (!_state.value.canInteract()) return

        _state.value = _state.value.copy(
            isLikePressed = true,
            isSwiping = isSwiping,
            exitTrigger = abs(_state.value.exitTrigger) + 1
        )
    }

    fun onHatePressed(isSwiping: Boolean = false) {
        if (!_state.value.canInteract()) return

        _state.value = _state.value.copy(
            isHatePressed = true,
            isSwiping = isSwiping,
            exitTrigger = (abs(_state.value.exitTrigger) + 1) * -1
        )
    }

    fun onExitFinished() {
        _state.value = _state.value.copy(
            currentCartoonIndex = _state.value.currentCartoonIndex + 1,
            isLikePressed = false,
            isHatePressed = false,
            isSwiping = false
        )
    }

    fun resetButtonStates() {
        _state.value = _state.value.copy(
            isLikePressed = false,
            isHatePressed = false
        )
    }
}