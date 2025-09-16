package com.ioi.myssue.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.analytics.AnalyticsLogger
import com.ioi.myssue.data.network.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val logger: AnalyticsLogger
) : ViewModel() {

    fun initApp(onReady: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val ok = authManager.registerNewDeviceIfNeeded()

            if (ok) {
                authManager.getUserId()?.let { id ->
                    Log.d("Splash Set UserID", "Set User ID: $id")
                    logger.setUserId(id)
                }
                onReady()
            } else onError()
        }
    }
}
