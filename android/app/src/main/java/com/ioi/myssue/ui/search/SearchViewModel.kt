package com.ioi.myssue.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.domain.repository.NewsRepository
import com.ioi.myssue.domain.model.NewsPage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val queryInput = MutableStateFlow("")

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        queryInput.value = q
    }

    fun onClearQuery() {
        onQueryChange("")
        refresh()
    }

    fun onSelectCategory(category: NewsCategory) {
        if (_state.value.selectedCat == category) return
        _state.update { it.copy(selectedCat = category) }
        refresh()
    }

    fun onSearch() {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(isInitialLoading = true, error = null) }

        runCatching {
            newsRepository.getNews(
                keyword = s.query.ifBlank { null },
                category = s.selectedCat.apiParam,
                size = s.pageSize,
                lastId = null
            )
        }.onSuccess { page: NewsPage ->
            _state.update {
                it.copy(
                    newsItems = page.newsItems,
                    lastId = page.lastId,
                    hasNext = page.hasNext,
                    isInitialLoading = false,
                )
            }
        }.onFailure { e ->
            _state.update { it.copy(isInitialLoading = false, error = e.message) }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.isInitialLoading || !s.hasNext) return

        viewModelScope.launch {
            _state.update { it.copy(error = null) }

            runCatching {
                newsRepository.getNews(
                    keyword = s.query.ifBlank { null },
                    category = s.selectedCat.apiParam,
                    size = s.pageSize,
                    lastId = s.lastId
                )
            }.onSuccess { page ->
                _state.update {
                    it.copy(
                        newsItems = it.newsItems + page.newsItems,
                        lastId = page.lastId,
                        hasNext = page.hasNext,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun onItemClick(id: Long) {
        _state.update { it.copy(selectedId = id) }
    }

    fun onItemClose() {
        _state.update { it.copy(selectedId = null) }
    }
}
