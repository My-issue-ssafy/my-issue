package com.ioi.myssue.ui.main

import androidx.lifecycle.ViewModel
import com.ioi.myssue.navigation.InternalRoute
import com.ioi.myssue.navigation.Navigator
import com.ioi.myssue.navigation.RouteSideEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class NavigatorViewModel @Inject constructor(
    navigator: Navigator,
) : ViewModel() {
    val sideEffect by lazy(LazyThreadSafetyMode.NONE) {
        navigator.channel.receiveAsFlow()
            .map { navigator ->
                when (navigator) {
                    is InternalRoute.Navigate ->
                        RouteSideEffect.Navigate(
                            navigator.route,
                            navigator.saveState,
                            navigator.launchSingleTop
                        )

                    is InternalRoute.NavigateBack -> RouteSideEffect.NavigateBack

                    is InternalRoute.NavigateAndClearBackStack ->
                        RouteSideEffect.NavigateAndClearBackStack(navigator.route)
                }
            }
    }
}
