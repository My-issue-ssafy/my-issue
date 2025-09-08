package com.ioi.myssue.ui.main

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.ioi.myssue.navigation.BottomTabRoute
import com.ioi.myssue.navigation.MainBottomBar
import com.ioi.myssue.navigation.MainTab
import com.ioi.myssue.ui.cartoon.CartoonScreen
import com.ioi.myssue.ui.mypage.MyPageScreen
import com.ioi.myssue.ui.news.NewsScreen
import com.ioi.myssue.ui.podcast.PodCastScreen
import com.ioi.myssue.ui.search.SearchScreen

@Composable
fun MainScreen(
    navBackStack: NavBackStack,
    onTabSelected: (MainTab) -> Unit,
) {
    val currentRoute = navBackStack.lastOrNull()
    val currentTab = when (currentRoute) {
        is BottomTabRoute.News -> MainTab.NEWS
        is BottomTabRoute.Search -> MainTab.SEARCH
        is BottomTabRoute.Cartoon -> MainTab.CARTOON
        is BottomTabRoute.Podcast -> MainTab.PODCAST
        is BottomTabRoute.MyPage -> MainTab.MYPAGE
        else -> MainTab.NEWS
    }

    Scaffold(
        bottomBar = {
            MainBottomBar(
                tabs = MainTab.entries.toList(),
                currentTab = currentTab,
                onTabSelected = onTabSelected,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        NavDisplay(
            entryDecorators = listOf(
                // Add the default decorators for managing scenes and saving state
                rememberSceneSetupNavEntryDecorator(),
                rememberSavedStateNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            backStack = navBackStack,
            onBack = { navBackStack.removeLastOrNull() },
            transitionSpec = {
                ContentTransform(
                    fadeIn(animationSpec = tween(0)),
                    fadeOut(animationSpec = tween(0)),
                )
            },
            popTransitionSpec = {
                ContentTransform(
                    fadeIn(animationSpec = tween(0)),
                    fadeOut(animationSpec = tween(0)),
                )
            },
            modifier = Modifier.padding(innerPadding),
            entryProvider = { key ->
                when (key) {
                    BottomTabRoute.News -> NavEntry(key) {
                        NewsScreen()
                    }

                    BottomTabRoute.Search -> NavEntry(key) {
                        SearchScreen()
                    }

                    BottomTabRoute.Cartoon -> NavEntry(key) {
                        CartoonScreen()
                    }

                    BottomTabRoute.Podcast -> NavEntry(key) {
                        PodCastScreen()
                    }

                    BottomTabRoute.MyPage -> NavEntry(key) {
                        MyPageScreen()
                    }

                    else -> NavEntry(key) { Unit }
                }
            },
        )
    }

}