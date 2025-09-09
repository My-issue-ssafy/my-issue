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

data class NewsItems(
    val hot: List<News> = emptyList(),
    val recommend: List<News> = emptyList(),
    val recent: List<News> = emptyList()
)

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val fakeNewsRepository: NewsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(NewsItems())
    val state = _state.asStateFlow()

    init {
        getNews()
    }

    fun getNews() { viewModelScope.launch {
        runCatching {
            val hotNews = fakeNewsRepository.getHotNews()
            val recommendedNews = fakeNewsRepository.getRecommendedNews()
            val recentNews = fakeNewsRepository.getRecentNews()
            Triple(hotNews, recommendedNews, recentNews)
        }
            .onSuccess { (hot, recommend, recent) ->
                _state.value = _state.value.copy(
                    hot = hot,
                    recommend = recommend,
                    recent = recent
                )
            }
            .onFailure {
            }
        }
        }
    }