package com.ioi.myssue.ui.cartoon

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.analytics.AnalyticsLogger
import com.ioi.myssue.domain.repository.CartoonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class CartoonViewModel @Inject constructor(
    private val cartoonRepository: CartoonRepository,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    private var _state = MutableStateFlow(CartoonUiState())
    val state = _state.asStateFlow()

    fun loadCartoon() = viewModelScope.launch {
        _state.update {
            it.copy(isLoading = true, error = null)
        }

        runCatching { cartoonRepository.getCartoonNews() }
            .onSuccess { cartoonNews ->
                _state.update {
                    it.copy(isLoading = false, cartoonNewsList = cartoonNews)
                }
            }
            .onFailure {
                Log.e("CartoonViewModel", "loadCartoon: ", it)
                _state.update {
                    it.copy(isLoading = false, error = "문제가 발생했습니다.\n다시 시도해주세요.")
                }
            }
    }

    fun onLikePressed(isSwiping: Boolean = false) {
        if (!_state.value.canInteract()) return

        _state.value = _state.value.copy(
            isLikePressed = true,
            isSwiping = isSwiping,
            exitTrigger = abs(_state.value.exitTrigger) + 1
        )

        viewModelScope.launch {
            _state.value.currentToonId?.let { toonId ->
                runCatching { cartoonRepository.likeCartoon(toonId) }
                    .onSuccess {
                        analyticsLogger.logToonPositive(toonId)
                    }
            }

        }
    }

    fun onHatePressed(isSwiping: Boolean = false) {
        if (!_state.value.canInteract()) return

        _state.value = _state.value.copy(
            isHatePressed = true,
            isSwiping = isSwiping,
            exitTrigger = (abs(_state.value.exitTrigger) + 1) * -1
        )

        viewModelScope.launch {
            _state.value.currentToonId?.let { toonId ->
                runCatching { cartoonRepository.hateCartoon(toonId) }
                    .onSuccess {
                        analyticsLogger.logToonNegative(toonId)
                    }
            }

        }
    }

    fun onExitFinished() {
        if (_state.value.isHatePressed || _state.value.isLikePressed) {
            _state.value =
                _state.value.copy(currentCartoonIndex = _state.value.currentCartoonIndex + 1)
        }
        resetState()
    }

    fun updateCardPositionX(tx: Float) {
        _state.update { it.copy(currentCardPositionX = tx) }
    }

    private fun resetState() {
        _state.value = _state.value.copy(
            isLikePressed = false,
            isHatePressed = false,
            isSwiping = false,
            exitTrigger = 0
        )
    }
}