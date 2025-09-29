package com.ioi.myssue.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.Channel

interface Navigator {

    val channel: Channel<InternalRoute>

    suspend fun navigate(
        route: NavKey,
        saveState: Boolean = false,
        launchSingleTop: Boolean = true
    )

    suspend fun navigateBack()

    suspend fun navigateAndClearBackStack(route: NavKey)
}
