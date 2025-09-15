package com.ioi.myssue.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.ioi.myssue.LocalAnalytics
import com.ioi.myssue.designsystem.theme.MyssueTheme
import com.ioi.myssue.navigation.BottomTabRoute
import com.ioi.myssue.analytics.AnalyticsLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var logger: AnalyticsLogger
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var startDestination: NavKey by remember { mutableStateOf(BottomTabRoute.News) }
            val navBackStack = rememberNavBackStack(startDestination)

            LaunchedNavigator(navBackStack)

            MyssueTheme {
                CompositionLocalProvider(LocalAnalytics provides logger) {
                    MainScreen(
                        navBackStack = navBackStack,
                        onTabSelected = {
                            when (it.route) {
                                BottomTabRoute.News -> viewModel.navigateNews()
                                BottomTabRoute.Search -> viewModel.navigateSearch()
                                BottomTabRoute.Cartoon -> viewModel.navigateCartoon()
                                BottomTabRoute.Podcast -> viewModel.navigatePodcast()
                                BottomTabRoute.MyPage -> viewModel.navigateMyPage()
                                is BottomTabRoute.NewsAll -> Unit
                            }
                        }
                    )
                }
            }
        }
    }
}