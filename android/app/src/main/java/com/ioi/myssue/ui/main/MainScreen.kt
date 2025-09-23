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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.designsystem.ui.AppTopBar
import com.ioi.myssue.designsystem.ui.TopBarMode
import com.ioi.myssue.designsystem.ui.TopBarViewModel
import com.ioi.myssue.navigation.BottomTabRoute
import com.ioi.myssue.navigation.MainBottomBar
import com.ioi.myssue.navigation.MainTab
import com.ioi.myssue.ui.cartoon.CartoonScreen
import com.ioi.myssue.ui.mypage.MyPageScreen
import com.ioi.myssue.ui.mypage.myscrap.MyScrapScreen
import com.ioi.myssue.ui.mypage.mytoon.MyCartoonScreen
import com.ioi.myssue.ui.news.NewsAllScreen
import com.ioi.myssue.ui.news.NewsScreen
import com.ioi.myssue.ui.notification.NotificationScreen
import com.ioi.myssue.ui.notification.NotificationViewModel
import com.ioi.myssue.ui.podcast.PodCastScreen
import com.ioi.myssue.ui.podcast.PodcastViewModel
import com.ioi.myssue.ui.search.SearchScreen

@Composable
fun MainScreen(
    navBackStack: NavBackStack,
    isTabSwitch: Boolean,
    onTabSelected: (MainTab) -> Unit,
) {
    val context = LocalContext.current
    val topBarViewModel: TopBarViewModel = hiltViewModel(context as MainActivity)

    // 현재 탭 계산
    val currentRoute = navBackStack.lastOrNull()
    val currentTab = mapTab(currentRoute, navBackStack)

    val atRoot = navBackStack.size <= 1

    val notificationState = topBarViewModel.notificationState.collectAsState().value
    val hasUnread = topBarViewModel.hasUnread.collectAsState().value

    // 알림 화면 진입 시 한 번만 상태 조회
    LaunchedEffect(currentRoute) {
        if (currentRoute == BottomTabRoute.Notification) {
            topBarViewModel.getNotificationStatus()
        }
    }


    BackHandler(enabled = atRoot) {
        if (currentTab != MainTab.NEWS) {
            onTabSelected(MainTab.NEWS)
        } else {
            context.finish()
        }
    }

    Scaffold(
        topBar = {
            val mode =
                if (currentRoute == BottomTabRoute.Notification)
                    TopBarMode.Notification
                else
                    TopBarMode.Default
            AppTopBar(
                onBack = if (MainTab.entries.find { it.route == currentRoute } == null) {
                    { navBackStack.removeLastOrNull() }
                } else null,
                onBellClick = { topBarViewModel.onNotificationClick() },
                mode = mode,
                notificationEnabled = notificationState ?: false,
                onToggleNotification = { to -> topBarViewModel.toggleNotification(to) },
                hasUnread = hasUnread
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
                rememberViewModelStoreNavEntryDecorator()
            ),
            backStack = navBackStack,
            onBack = { navBackStack.removeLastOrNull() },
            transitionSpec = { if (isTabSwitch) noAnim() else slideLeft() },
            popTransitionSpec = { if (isTabSwitch) noAnim() else slideRight() },
            modifier = Modifier.padding(innerPadding),
            entryProvider = { key ->
                when (key) {
                    BottomTabRoute.News -> NavEntry(key) { NewsScreen() }
                    is BottomTabRoute.NewsAll -> NavEntry(key) { NewsAllScreen(type = key.type) }
                    BottomTabRoute.Search -> NavEntry(key) { SearchScreen() }
                    BottomTabRoute.Cartoon -> NavEntry(key) { CartoonScreen() }
                    BottomTabRoute.Podcast -> NavEntry(key) {
                        val podcastViewModel: PodcastViewModel = hiltViewModel(context)
                        PodCastScreen(podcastViewModel)
                    }
                    BottomTabRoute.MyPage -> NavEntry(key) { MyPageScreen() }
                    BottomTabRoute.MyScrap -> NavEntry(key) { MyScrapScreen() }
                    BottomTabRoute.MyCartoon -> NavEntry(key) { MyCartoonScreen() }
                    BottomTabRoute.Notification -> NavEntry(key) {
                        val notificationViewModel: NotificationViewModel = hiltViewModel(context)
                        NotificationScreen(notificationViewModel)
                    }
                    else -> NavEntry(key) { Unit }
                }
            },
        )
    }
}

private fun AnimatedContentTransitionScope<*>.slideAnim(
    to: AnimatedContentTransitionScope.SlideDirection,
    durationMillis: Int = 400
): ContentTransform =
    slideIntoContainer(to, animationSpec = tween(durationMillis)) togetherWith
            slideOutOfContainer(to, animationSpec = tween(durationMillis))

// 편의 헬퍼
private fun AnimatedContentTransitionScope<*>.slideLeft(durationMillis: Int = 400) =
    slideAnim(AnimatedContentTransitionScope.SlideDirection.Left, durationMillis)

private fun AnimatedContentTransitionScope<*>.slideRight(durationMillis: Int = 400) =
    slideAnim(AnimatedContentTransitionScope.SlideDirection.Right, durationMillis)

private fun noAnim(): ContentTransform =
    fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))

private fun mapTab(
    currentRoute: NavKey?,
    navBackStack: NavBackStack,
    depth: Int = 1
): MainTab = when (currentRoute) {
    is BottomTabRoute.News, is BottomTabRoute.NewsAll -> MainTab.NEWS
    is BottomTabRoute.Search -> MainTab.SEARCH
    is BottomTabRoute.Cartoon -> MainTab.CARTOON
    is BottomTabRoute.Podcast -> MainTab.PODCAST
    is BottomTabRoute.MyPage, is BottomTabRoute.MyScrap, is BottomTabRoute.MyCartoon -> MainTab.MYPAGE
    else -> mapTab(navBackStack[navBackStack.size - 1 - depth], navBackStack, depth+1)
}