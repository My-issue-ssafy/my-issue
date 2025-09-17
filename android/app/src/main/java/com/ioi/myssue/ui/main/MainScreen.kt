package com.ioi.myssue.ui.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.designsystem.ui.AppTopBar
import com.ioi.myssue.navigation.BottomTabRoute
import com.ioi.myssue.navigation.MainBottomBar
import com.ioi.myssue.navigation.MainTab
import com.ioi.myssue.ui.cartoon.CartoonScreen
import com.ioi.myssue.ui.mypage.mytoon.MyCartoonScreen
import com.ioi.myssue.ui.mypage.MyPageScreen
import com.ioi.myssue.ui.mypage.myscrap.MyScrapScreen
import com.ioi.myssue.ui.news.NewsAllScreen
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
        is BottomTabRoute.News, is BottomTabRoute.NewsAll  -> MainTab.NEWS
        is BottomTabRoute.Search -> MainTab.SEARCH
        is BottomTabRoute.Cartoon -> MainTab.CARTOON
        is BottomTabRoute.Podcast -> MainTab.PODCAST
        is BottomTabRoute.MyPage, is BottomTabRoute.MyScrap, is BottomTabRoute.MyCartoon -> MainTab.MYPAGE
        else -> null
    }

    Scaffold(
        topBar = {
            AppTopBar()
        },
        bottomBar = {
            MainBottomBar(
                tabs = MainTab.entries.toList(),
                currentTab = currentTab,
                onTabSelected = onTabSelected,
            )
        },
        containerColor = BackgroundColors.Background100,
    ) { innerPadding ->
        NavDisplay(
            entryDecorators = listOf(
                rememberSceneSetupNavEntryDecorator(),
//                rememberSavedStateNavEntryDecorator(),
//                rememberViewModelStoreNavEntryDecorator()
            ),
            backStack = navBackStack,
            onBack = { navBackStack.removeLastOrNull() },
            transitionSpec = {noAnim()},
            popTransitionSpec = {noAnim()},
            modifier = Modifier.padding(innerPadding),
            entryProvider = { key ->
                when (key) {
                    BottomTabRoute.News -> NavEntry(key,
                        ) {
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

                    BottomTabRoute.MyScrap -> NavEntry(
                        key = key,
                        metadata = slideAnimationMetaData()
                    ) {
                        MyScrapScreen()
                    }

                    BottomTabRoute.MyCartoon -> NavEntry(
                        key = key,
                        metadata = slideAnimationMetaData()
                    ) {
                        MyCartoonScreen()
                    }

                    is BottomTabRoute.NewsAll -> NavEntry(
                        key = key,
                        metadata = slideAnimationMetaData()
                    ) {
                        NewsAllScreen(type = key.type)
                    }

                    else -> NavEntry(key) { Unit }
                }
            },
        )
    }
}

private fun slideAnimationMetaData(): Map<String, Any> = NavDisplay.transitionSpec {
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(400)
    ) togetherWith slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(400)
    )
} + NavDisplay.popTransitionSpec {
    fadeIn(animationSpec = tween(0)) togetherWith slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(400)
    )
}

private fun noAnim() =
    fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))