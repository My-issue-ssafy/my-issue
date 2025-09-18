package com.ioi.myssue.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplay.popTransitionSpec
import androidx.navigation3.ui.NavDisplay.transitionSpec
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.designsystem.ui.AppTopBar
import com.ioi.myssue.navigation.BottomTabRoute
import com.ioi.myssue.navigation.MainBottomBar
import com.ioi.myssue.navigation.MainTab
import com.ioi.myssue.ui.cartoon.CartoonScreen
import com.ioi.myssue.ui.mypage.MyPageScreen
import com.ioi.myssue.ui.mypage.myscrap.MyScrapScreen
import com.ioi.myssue.ui.mypage.mytoon.MyCartoonScreen
import com.ioi.myssue.ui.news.NewsAllScreen
import com.ioi.myssue.ui.news.NewsScreen
import com.ioi.myssue.ui.podcast.PodCastScreen
import com.ioi.myssue.ui.search.SearchScreen

@Composable
fun MainScreen(
    navBackStack: NavBackStack,
    isTabSwitch: Boolean,
    onTabSelected: (MainTab) -> Unit,
) {
    val context = LocalContext.current

    // 현재 탭 계산
    val currentRoute = navBackStack.lastOrNull()
    val currentTab = when (currentRoute) {
        is BottomTabRoute.News, is BottomTabRoute.NewsAll -> MainTab.NEWS
        is BottomTabRoute.Search -> MainTab.SEARCH
        is BottomTabRoute.Cartoon -> MainTab.CARTOON
        is BottomTabRoute.Podcast -> MainTab.PODCAST
        is BottomTabRoute.MyPage, is BottomTabRoute.MyScrap, is BottomTabRoute.MyCartoon -> MainTab.MYPAGE
        else -> null
    }

    val atRoot = navBackStack.size <= 1
    BackHandler(enabled = atRoot) {
        if (currentTab != MainTab.NEWS) {
            onTabSelected(MainTab.NEWS)
        } else {
            (context as MainActivity).finish()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                onBack = if (MainTab.entries.find { it.route == currentRoute } == null) {
                    { navBackStack.removeLastOrNull() }
                } else null,
            )
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
                rememberSavedStateNavEntryDecorator(),
            ),
            backStack = navBackStack,
            // 🔹 루트가 아닐 때의 Back은 NavDisplay에게 맡김 → 탭 내부 팝
            onBack = { navBackStack.removeLastOrNull() },
            // 🔹 탭 전환(noAnim) vs 내부 이동(slideAnim)
            transitionSpec = { if (isTabSwitch) noAnim() else slideAnim() },
            popTransitionSpec = { if (isTabSwitch) noAnim() else slideAnim() },
            modifier = Modifier.padding(innerPadding),
            entryProvider = { key ->
                when (key) {
                    BottomTabRoute.News -> NavEntry(key) { NewsScreen() }
                    is BottomTabRoute.NewsAll -> NavEntry(key) { NewsAllScreen(type = key.type) }
                    BottomTabRoute.Search -> NavEntry(key) { SearchScreen() }
                    BottomTabRoute.Cartoon -> NavEntry(key) { CartoonScreen() }
                    BottomTabRoute.Podcast -> NavEntry(key) { PodCastScreen() }
                    BottomTabRoute.MyPage -> NavEntry(key) { MyPageScreen() }
                    BottomTabRoute.MyScrap -> NavEntry(key) { MyScrapScreen() }
                    BottomTabRoute.MyCartoon -> NavEntry(key) { MyCartoonScreen() }
                    else -> NavEntry(key) { Unit }
                }
            },
        )
    }
}

private fun AnimatedContentTransitionScope<*>.slideAnim(): ContentTransform =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(400)
    ) togetherWith slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(400)
    )

private fun noAnim(): ContentTransform =
    fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
