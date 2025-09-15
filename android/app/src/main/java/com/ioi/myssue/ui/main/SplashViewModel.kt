package com.ioi.myssue.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.data.network.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    fun initApp(onReady: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val ok = authManager.registerNewDeviceIfNeeded()
            if (ok) onReady() else onError()
        }
    }
}
