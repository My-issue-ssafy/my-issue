package com.ioi.myssue.ui.main

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.NavBackStack
import com.ioi.myssue.navigation.BottomTabRoute
import com.ioi.myssue.navigation.MainTab
import com.ioi.myssue.navigation.RouteSideEffect
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LaunchedNavigator(
    tabBackStacks: Map<MainTab, NavBackStack>,
    currentTab: MainTab,
    onTabChange: (MainTab) -> Unit,
    routerViewModel: NavigatorViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(routerViewModel, lifecycleOwner, currentTab) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            routerViewModel.sideEffect.collectLatest { sideEffect ->
                when (sideEffect) {
                    is RouteSideEffect.NavigateBack -> {
                        tabBackStacks[currentTab]?.removeLastOrNull()
                    }
                    is RouteSideEffect.Navigate -> {
                        val targetTab = findTabForRoute(sideEffect.route)
                        if (targetTab != null) {
                            if (currentTab != targetTab) onTabChange(targetTab)
                            val backStack = tabBackStacks[targetTab] ?: return@collectLatest
                            backStack.remove(sideEffect.route)
                            backStack.add(sideEffect.route)
                        }
                    }
                    is RouteSideEffect.NavigateAndClearBackStack -> {
                        val targetTab = findTabForRoute(sideEffect.route)
                        if (targetTab != null) {
                            if (currentTab != targetTab) onTabChange(targetTab)
                            val backStack = tabBackStacks[targetTab] ?: return@collectLatest
                            backStack.clear()
                            backStack.add(sideEffect.route)
                        }
                    }
                }
            }
        }
    }
}

fun findTabForRoute(route: Any): MainTab? {
    return when (route) {
        is BottomTabRoute.News, is BottomTabRoute.NewsAll -> MainTab.NEWS
        is BottomTabRoute.Search -> MainTab.SEARCH
        is BottomTabRoute.Cartoon -> MainTab.CARTOON
        is BottomTabRoute.Podcast -> MainTab.PODCAST
        is BottomTabRoute.MyPage, is BottomTabRoute.MyScrap, is BottomTabRoute.MyCartoon -> MainTab.MYPAGE
        else -> null
    }
}
