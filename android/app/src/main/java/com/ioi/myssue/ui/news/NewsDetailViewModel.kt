package com.ioi.myssue.ui.news

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.domain.repository.NewsRepository
import com.ioi.myssue.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "NewsDetailViewModel"
sealed interface NewsDetailEffect {
    data class Toast(val message: String): NewsDetailEffect
}
@HiltViewModel
class NewsDetailViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val navigator: Navigator
) : ViewModel() {
    private val _uiState = MutableStateFlow(NewsDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<NewsDetailEffect>()
    val effect = _effect.asSharedFlow()

    fun getNewsDetail(newsId: Long?) {
        if(newsId == null) return
        viewModelScope.launch {
            _uiState.update { NewsDetailUiState(loading = true, error = null) }
            runCatching { newsRepository.getNewsDetail(newsId) }
                .onSuccess { news ->
                    Log.d(TAG, "getNewsDetail: $newsId")
                    _uiState.update {
                        it.copy(
                            loading = false,
                            newsId = news.newsId,
                            title = news.title,
                            author = news.author,
                            thumbnail = news.thumbnail,
                            newspaper = news.newspaper,
                            displayTime = news.displayTime,
                            blocks = news.content,
                            scrapCount = news.scrapCount,
                            isBookmarked = news.isScraped
                        )
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "getNewsDetail failed: id=$newsId, ${e::class.simpleName}: ${e.message}", e)
                }
        }
    }

    fun toggleBookmark() {
        val id = _uiState.value.newsId
        val before = _uiState.value.isBookmarked
        val next = !before

        _uiState.update { it.copy(isBookmarked = next) }

        viewModelScope.launch {
            runCatching { newsRepository.bookMarkNews(id) }
                .onSuccess { confirmed ->
                    _uiState.update { it.copy(isBookmarked = confirmed) }
                    _effect.emit(
                        NewsDetailEffect.Toast(
                            if (confirmed) "뉴스를 스크랩 했어요" else "스크랩을 해제했어요"
                        )
                    )
                }
                .onFailure { }
        }
    }

    fun openChat() = viewModelScope.launch {
        _uiState.update { it.copy(c = true) }
    }

    fun closeChat() = viewModelScope.launch {
        _uiState.update { it.copy(c = false) }
    }
}