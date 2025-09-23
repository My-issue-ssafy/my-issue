package com.ioi.myssue.ui.main

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation3.runtime.rememberNavBackStack
import com.ioi.myssue.LocalAnalytics
import com.ioi.myssue.analytics.AnalyticsLogger
import com.ioi.myssue.designsystem.theme.MyssueTheme
import com.ioi.myssue.navigation.BottomTabRoute
import com.ioi.myssue.navigation.MainTab
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var logger: AnalyticsLogger

    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            splashViewModel.isLoading.value
        }

        enableEdgeToEdge()
        setContent {
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

            // ViewModel 사이드이펙트 네비게이터 (탭 바뀌면 무애니 처리)
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

            MyssueTheme {
                CompositionLocalProvider(LocalAnalytics provides logger) {
                    MainScreen(
                        navBackStack = tabBackStacks[currentTab] ?: newsBackStack,
                        onTabSelected = { newTab ->
                            if (currentTab != newTab) {
                                isTabSwitch = true
                                currentTab = newTab
                                Log.d("MainActivity", "Tab changed to $currentTab")
                            }
                        },
                        isTabSwitch = isTabSwitch
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
