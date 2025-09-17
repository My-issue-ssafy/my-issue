package com.ioi.myssue.ui.mypage.mytoon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.domain.model.CartoonNews
import com.ioi.myssue.domain.repository.CartoonRepository
import com.ioi.myssue.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyCartoonViewModel @Inject constructor(
    private val cartoonRepository: CartoonRepository,
    private val navigator: Navigator
): ViewModel() {

    private var _state = MutableStateFlow(MyCartoonUiState())
    val state = _state.onSubscription {
        initData()
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5_000),
        initialValue = MyCartoonUiState()
    )

    private fun initData() {
        loadMyToons()
    }

    private fun loadMyToons() = viewModelScope.launch {
        runCatching { cartoonRepository.getLikedCartoons(null, 10) }
            .onSuccess { cartoonNews ->
                _state.update {
                    it.copy(myToons = cartoonNews)
                }
            }
    }

    fun setClickedToon(toon: CartoonNews?) {
        _state.update {
            it.copy(clickedToon = toon)
        }
    }
}

data class MyCartoonUiState(
    val myToons: List<CartoonNews> = emptyList(),
    val clickedToon: CartoonNews? = null,
)