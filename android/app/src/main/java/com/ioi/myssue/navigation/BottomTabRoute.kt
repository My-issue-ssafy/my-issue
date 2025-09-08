package com.ioi.myssue.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface BottomTabRoute: NavKey {

    @Serializable
    data object News: BottomTabRoute

    @Serializable
    data object Search: BottomTabRoute

    @Serializable
    data object Cartoon: BottomTabRoute

    @Serializable
    data object Podcast: BottomTabRoute

    @Serializable
    data object MyPage: BottomTabRoute
}