package com.ioi.myssue.ui.mypage.myscrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.domain.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyScrapViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
): ViewModel() {

    private var _state = MutableStateFlow(MyScrapUiState())
    val state = _state.asStateFlow()

    fun initData() {
        loadMyScraps()
    }

    fun loadMyScraps() {viewModelScope.launch {
        if(!_state.value.hasNext) return@launch
        _state.update { it.copy(isLoading = true) }
            runCatching { newsRepository.getBookmarkedNews(cursor = _state.value.cursor) }
                .onSuccess { cursorPage ->
                    _state.update {
                        it.copy(
                            newsSummaries = it.newsSummaries + cursorPage.items,
                            cursor = cursorPage.nextCursor,
                            hasNext = cursorPage.hasNext,
                            isLoading = false
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false) }
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
