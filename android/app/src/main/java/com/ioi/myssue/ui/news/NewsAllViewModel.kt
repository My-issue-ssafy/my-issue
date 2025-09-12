package com.ioi.myssue.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.domain.model.News
import com.ioi.myssue.domain.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HotNewsItems(
    val news: List<News> = emptyList()
)

@HiltViewModel
class NewsHotViewModel @Inject constructor(
    private val fakeNewsRepository: NewsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HotNewsItems())
    val state = _state.asStateFlow()

    fun getHotNews() {
        viewModelScope.launch {
            runCatching {
                fakeNewsRepository.getHotNews()
            }
                .onSuccess { list ->
                    _state.value = HotNewsItems(news = list)
                }
                .onFailure {
                }
        }
    }
}