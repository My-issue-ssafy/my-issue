package com.ioi.myssue.ui.news

import android.util.Log
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

    private val _state = MutableStateFlow(NewsAllUiState())
    val state = _state.asStateFlow()

    fun getAllHotNews(cursor: String? = _state.value.nextCursor, pageSize: Int = 10) =
        loadNews(cursor) { newsRepository.getHotNews(cursor, pageSize) }

    fun getAllRecommendNews(cursor: String? = _state.value.nextCursor, pageSize: Int = 10) =
        loadNews(cursor) { newsRepository.getRecommendNews(cursor, pageSize) }

    fun getAllRecentNews(cursor: String? = _state.value.nextCursor, size: Int = 10) =
        loadNews(cursor) { newsRepository.getTrendNews(cursor, size) }

    private fun loadNews(
        cursor: String?,
        call: suspend () -> CursorPage<NewsSummary>
    ) = viewModelScope.launch {
        val firstPage = cursor == null && _state.value.items.isEmpty()
        if (firstPage) _state.value = _state.value.copy(isInitialLoading = true)

        runCatching { call() }
            .onSuccess { page ->
                _state.value = _state.value.copy(
                    items = if (cursor == null) page.items else _state.value.items + page.items,
                    nextCursor = page.nextCursor,
                    hasNext = page.hasNext,
                    isInitialLoading = false
                )
            }
            .onFailure { e ->
                Log.e("NewsAllViewModel", "load failed", e)
                if (firstPage) _state.value = _state.value.copy(isInitialLoading = false)
            }
    }

}