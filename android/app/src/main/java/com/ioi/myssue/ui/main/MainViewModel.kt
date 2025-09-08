package com.ioi.myssue.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.navigation.BottomTabRoute
import com.ioi.myssue.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class MainViewModel @Inject constructor(
    private val navigator: Navigator,
) : ViewModel() {
    
    fun navigateNews() = viewModelScope.launch {
        navigator.navigate(
            route = BottomTabRoute.News,
            saveState = true,
            launchSingleTop = true,
        )
    }

    fun navigateSearch() = viewModelScope.launch {
        navigator.navigate(
            route = BottomTabRoute.Search,
            saveState = true,
            launchSingleTop = true,
        )
    }

    fun navigateCartoon() = viewModelScope.launch {
        navigator.navigate(
            route = BottomTabRoute.Cartoon,
            saveState = true,
            launchSingleTop = true,
        )
    }

    fun navigatePodcast() = viewModelScope.launch {
        navigator.navigate(
            route = BottomTabRoute.Podcast,
            saveState = true,
            launchSingleTop = true,
        )
    }

    fun navigateMyPage() = viewModelScope.launch {
        navigator.navigate(
            route = BottomTabRoute.MyPage,
            saveState = true,
            launchSingleTop = true,
        )
    }
}