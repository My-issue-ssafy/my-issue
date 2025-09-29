package com.ioi.myssue.ui.mypage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.domain.repository.CartoonRepository
import com.ioi.myssue.domain.repository.NewsRepository
import com.ioi.myssue.navigation.BottomTabRoute
import com.ioi.myssue.navigation.Navigator
import com.ioi.myssue.ui.mypage.myscrap.MyScrapUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val cartoonRepository: CartoonRepository,
    private val navigator: Navigator
) : ViewModel() {

    private var _state = MutableStateFlow(MyPageUiState())
    val state = _state.onSubscription {
        initData()
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5_000),
        initialValue = MyPageUiState()
    )

    private fun initData() {
        _state.update { it.copy(isLoadingScrapNews = true, isLoadingLikeToons = true) }
        loadMyScraps()
        loadMyToons()
    }

    fun loadMyScraps() = viewModelScope.launch {
        runCatching { newsRepository.getBookmarkedNews(cursor = null) }
            .onSuccess { cursorPage ->
                _state.update {
                    it.copy(
                        newsSummaries = cursorPage.items.take(15),
                        isLoadingScrapNews = false
                    )
                }
            }
            .onFailure {
                _state.update { it.copy(isLoadingScrapNews = false) }
            }
    }

    fun loadMyToons() = viewModelScope.launch {
        runCatching { cartoonRepository.getLikedCartoons() }
            .onSuccess { cartoonNews ->
                _state.update {
                    it.copy(myToons = cartoonNews.take(5), isLoadingLikeToons = false)
                }
            }
            .onFailure {
                _state.update { it.copy(isLoadingLikeToons = false) }
            }
    }

    fun navigateToAllScraps() = viewModelScope.launch {
        navigator.navigate(BottomTabRoute.MyScrap)
    }

    fun navigateToAllToons() = viewModelScope.launch {
        navigator.navigate(BottomTabRoute.MyCartoon)
    }

    fun navigateToNews() = viewModelScope.launch {
        navigator.navigate(BottomTabRoute.News)
    }

    fun navigateToCartoonNews() = viewModelScope.launch {
        navigator.navigate(BottomTabRoute.Cartoon)
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
