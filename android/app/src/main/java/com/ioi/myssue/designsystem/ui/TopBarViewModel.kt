package com.ioi.myssue.designsystem.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.data.repository.NotificationRepositoryImpl
import com.ioi.myssue.navigation.BottomTabRoute
import com.ioi.myssue.navigation.Navigator
import com.ioi.myssue.player.AudioController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopBarViewModel @Inject constructor(
    private val audioController: AudioController,
    private val navigator: Navigator,
    private val notificationRepository: NotificationRepositoryImpl
) : ViewModel() {
    private val _notificationState = MutableStateFlow<Boolean?>(null)
    val notificationState: StateFlow<Boolean?> = _notificationState.asStateFlow()

    private val _hasUnread = MutableStateFlow(false)
    val hasUnread: StateFlow<Boolean> = _hasUnread.asStateFlow()

    init {
        viewModelScope.launch {
            audioController.audioState.collect { audio ->
                if (audio.isPlaying) {
                }
            }
        }
        refreshUnread()
    }

    fun play() = audioController.play()
    fun pause() = audioController.pause()

    fun onNotificationClick() = viewModelScope.launch {
        navigator.navigate(BottomTabRoute.Notification)
    }

    fun getNotificationStatus() = viewModelScope.launch {
        _notificationState.value = notificationRepository.getStatus()
    }

    fun refreshUnread() = viewModelScope.launch {
        runCatching { notificationRepository.getUnreadNotification() }
            .onSuccess { _hasUnread.value = it }
            .onFailure {  }
    }

    fun toggleNotification(to: Boolean) = viewModelScope.launch {
        _notificationState.value = to
        runCatching { notificationRepository.setStatus() }
            .onFailure {}
    }
}
