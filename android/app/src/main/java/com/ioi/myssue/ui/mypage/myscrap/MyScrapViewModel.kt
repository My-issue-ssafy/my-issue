package com.ioi.myssue.ui.mypage.myscrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.domain.model.CartoonNews
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.domain.repository.NewsRepository
import com.ioi.myssue.navigation.BottomTabRoute
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
class MyScrapViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val navigator: Navigator
): ViewModel() {

    private var _state = MutableStateFlow(MyPageUiState())
    val state = _state.onSubscription {
        initData()
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5_000),
        initialValue = MyPageUiState()
    )

    private fun initData() {
        loadMyScraps()
    }

    private fun loadMyScraps() {viewModelScope.launch {
            runCatching { newsRepository.getBookmarkedNews(lastId = null) }
                .onSuccess { cursorPage ->
                    _state.update {
                        it.copy(
                            newsSummaries = cursorPage.items,
                        )
                    }
                }
        }
    }

    fun openNewsDetail(newsId: Long) = viewModelScope.launch {
        _state.update {
            it.copy(selectedNewsId = newsId)
        }
    }

    fun closeNewsDetail() = viewModelScope.launch {
        _state.update {
            it.copy(selectedNewsId = null)
        }
    }
}

data class MyPageUiState(
    val isNotificationEnabled: Boolean = true,
    val newsSummaries: List<NewsSummary> = emptyList(),
    val myToons: List<CartoonNews> = emptyList(),
    val selectedNewsId: Long? = null,
)