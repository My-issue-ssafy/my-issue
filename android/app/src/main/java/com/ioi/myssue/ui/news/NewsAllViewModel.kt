package com.ioi.myssue.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.domain.model.CursorPage
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.domain.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsAllViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CursorPage(items = emptyList<NewsSummary>(), nextCursor = null, hasNext = false))
    val state = _state.asStateFlow()

    fun getAllHotNews(cursor: String? = _state.value.nextCursor, pageSize: Int = 10) =
        viewModelScope.launch {
            runCatching {
                newsRepository.getHotNews(cursor, pageSize)
            }
                .onSuccess { page ->
                    _state.value = _state.value.copy(
                        items = if (cursor == null) page.items else _state.value.items + page.items,
                        nextCursor = page.nextCursor,
                    )
                }
                .onFailure {
                }
        }

    fun getAllRecommendNews(cursor: String? = _state.value.nextCursor, pageSize: Int = 10) =
        viewModelScope.launch {
            runCatching {
                newsRepository.getRecommendNews(cursor, pageSize)
            }
                .onSuccess { page ->
                    _state.value = _state.value.copy(
                        items = if (cursor == null) page.items else _state.value.items + page.items,
                        nextCursor = page.nextCursor,
                    )
                }
                .onFailure {
                }
        }

    fun getAllRecentNews(cursor: String? = _state.value.nextCursor, size: Int = 10) =
        viewModelScope.launch {
            runCatching {
                newsRepository.getTrendNews(cursor, size)
            }
                .onSuccess { page ->
                    _state.value = _state.value.copy(
                        items = if (cursor == null) page.items else _state.value.items + page.items,
                        nextCursor = page.nextCursor,
                    )
                }
                .onFailure {
                }
        }
}