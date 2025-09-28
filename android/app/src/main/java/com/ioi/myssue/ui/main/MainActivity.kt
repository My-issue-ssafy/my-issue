package com.ioi.myssue.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack
import com.ioi.myssue.LocalAnalytics
import com.ioi.myssue.analytics.AnalyticsLogger
import com.ioi.myssue.designsystem.theme.MyssueTheme
import com.ioi.myssue.designsystem.ui.TopBarViewModel
import com.ioi.myssue.navigation.BottomTabRoute
import com.ioi.myssue.navigation.MainTab
import com.ioi.myssue.permissions.NotificationPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

val LocalDeepLinkNewsId = compositionLocalOf<Long?> { null }
val LocalConsumeDeepLinkNewsId = compositionLocalOf<() -> Unit> { {} }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var logger: AnalyticsLogger
    private val splashViewModel: SplashViewModel by viewModels()
    private val deepLinkNewsId = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            splashViewModel.isLoading.value
        }

        deepLinkNewsId.value = parseDeepLink(intent)

        enableEdgeToEdge()
        setContent {
            val splashLoading by splashViewModel.isLoading.collectAsState()
            if (!splashLoading) {
                var firstLaunch by rememberSaveable { mutableStateOf(true) }

                val newsBackStack = rememberNavBackStack(BottomTabRoute.News)
                val searchBackStack = rememberNavBackStack(BottomTabRoute.Search)
                val cartoonBackStack = rememberNavBackStack(BottomTabRoute.Cartoon)
                val podcastBackStack = rememberNavBackStack(BottomTabRoute.Podcast)
                val myPageBackStack = rememberNavBackStack(BottomTabRoute.MyPage)

                val tabBackStacks = mapOf(
                    MainTab.NEWS to newsBackStack,
                    MainTab.SEARCH to searchBackStack,
                    MainTab.CARTOON to cartoonBackStack,
                    MainTab.PODCAST to podcastBackStack,
                    MainTab.MYPAGE to myPageBackStack
                )

                var currentTab by rememberSaveable { mutableStateOf(MainTab.NEWS) }
                var isTabSwitch by remember { mutableStateOf(false) }

                val pendingNewsId by deepLinkNewsId.collectAsState()

                LaunchedNavigator(
                    tabBackStacks = tabBackStacks,
                    currentTab = currentTab,
                    onTabChange = { newTab ->
                        if (currentTab != newTab) {
                            isTabSwitch = true
                            currentTab = newTab
                        }
                    }
                )

                val topBarVm: TopBarViewModel = hiltViewModel()

                NotificationPermission(
                    firstLaunch = firstLaunch,
                    onNewGranted = {
                        topBarVm.toggleNotification(true)
                        firstLaunch = false
                    },
                    onRefused = {
                        firstLaunch = false
                    },
                    onAlreadyGranted = {
                        firstLaunch = false
                    }
                )

                MyssueTheme {
                    CompositionLocalProvider(
                        LocalAnalytics provides logger,
                        LocalDeepLinkNewsId provides pendingNewsId,
                        LocalConsumeDeepLinkNewsId provides { deepLinkNewsId.value = null }
                    ) {
                        LaunchedEffect(pendingNewsId) {
                            if (pendingNewsId != null && currentTab != MainTab.NEWS) {
                                tabBackStacks[currentTab]?.popSomeScreenIfTop()
                                isTabSwitch = true
                                currentTab = MainTab.NEWS
                                Log.d("MainActivity", "DeepLink → switch to NEWS")
                            }
                        }

                        MainScreen(
                            navBackStack = tabBackStacks[currentTab] ?: newsBackStack,
                            onTabSelected = { newTab ->
                                tabBackStacks[currentTab]?.popSomeScreenIfTop()
                                isTabSwitch = true
                                currentTab = newTab
                                if (currentTab == MainTab.NEWS) {
                                    newsBackStack.removeIf { it is BottomTabRoute.NewsAll }
                                }
                                Log.d("MainActivity", "Tab changed to $currentTab")
                            },
                            isTabSwitch = isTabSwitch,
                            topBarViewModel = topBarVm
                        )
                    }
                }

                LaunchedEffect(currentTab) {
                    withFrameNanos {}
                    isTabSwitch = false
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkNewsId.value = parseDeepLink(intent)
    }

    private fun parseDeepLink(intent: Intent?): Long? {
        val u: Uri = intent?.data ?: return null
        return if (u.scheme == "myssue" && u.host == "news") {
            u.lastPathSegment?.toLongOrNull()
        } else null
    }

    private fun NavBackStack.popSomeScreenIfTop() {
        while (lastOrNull() == BottomTabRoute.Notification || lastOrNull() == BottomTabRoute.NewsAll) {
            removeLastOrNull()
        }
    }
}