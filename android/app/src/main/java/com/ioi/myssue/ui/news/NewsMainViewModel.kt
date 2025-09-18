package com.ioi.myssue.ui.news

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.domain.model.MainNewsList
import com.ioi.myssue.domain.model.News
import com.ioi.myssue.domain.repository.NewsRepository
import com.ioi.myssue.navigation.BottomTabRoute
import com.ioi.myssue.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsMainViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val navigator: Navigator
) : ViewModel() {
    private val _state = MutableStateFlow(NewsMainUiState(isInitialLoading = true))
    val state = _state.asStateFlow()

    init {
        getNews()
    }

    fun getNews() {
        val firstPage = _state.value.main.hot.isEmpty() &&
                _state.value.main.recommend.isEmpty() &&
                _state.value.main.latest.isEmpty()
        if (firstPage) _state.value = _state.value.copy(isInitialLoading = true)

        viewModelScope.launch {
            runCatching { newsRepository.getMainNews() }
                .onSuccess { main ->
                    _state.value = _state.value.copy(
                        main = main,
                        isInitialLoading = false
                    )
                }
                .onFailure {
                    Log.d("NewsMainViewModel", "getNews: ${it}")
                    if (firstPage) _state.value = _state.value.copy(isInitialLoading = false)
                }
        }
    }

    fun onItemClick(id: Long) {
        _state.update { it.copy(selectedId = id) }
    }

    fun onItemClose() {
        _state.update { it.copy(selectedId = null) }
    }

    fun onClickSeeAll(type: NewsFeedType) = viewModelScope.launch {
            navigator.navigate(BottomTabRoute.NewsAll(type))
        }
}