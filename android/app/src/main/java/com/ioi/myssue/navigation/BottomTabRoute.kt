package com.ioi.myssue.navigation

import androidx.navigation3.runtime.NavKey
import com.ioi.myssue.ui.news.NewsFeedType
import kotlinx.serialization.Serializable

sealed interface BottomTabRoute: NavKey {

    @Serializable
    data object News: BottomTabRoute

    @Serializable
    data class NewsAll(val type: NewsFeedType) : BottomTabRoute

    @Serializable
    data object Search: BottomTabRoute

    @Serializable
    data object Cartoon: BottomTabRoute

    @Serializable
    data object Podcast: BottomTabRoute

    @Serializable
    data object MyPage: BottomTabRoute

    @Serializable
    data object MyScrap: BottomTabRoute

    @Serializable
    data object MyCartoon: BottomTabRoute
}