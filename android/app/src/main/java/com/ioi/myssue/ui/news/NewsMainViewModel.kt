package com.ioi.myssue.ui.news

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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsMainViewModel @Inject constructor(
    private val fakeNewsRepository: NewsRepository,
    private val navigator: Navigator
) : ViewModel() {
    private val _state = MutableStateFlow(MainNewsList())
    val state = _state.asStateFlow()

    init {
        getNews()
    }

    fun getNews() {
        viewModelScope.launch {
            runCatching { fakeNewsRepository.getMainNews() }
                .onSuccess { main ->
                    _state.value = MainNewsList(
                        hot = main.hot, recommend = main.recommend, recent = main.recent
                    )
                }
                .onFailure {
                }
        }
    }

    fun onClickSeeAll(type: NewsFeedType) {
        viewModelScope.launch {
            navigator.navigate(BottomTabRoute.NewsAll(type))
        }
    }
}