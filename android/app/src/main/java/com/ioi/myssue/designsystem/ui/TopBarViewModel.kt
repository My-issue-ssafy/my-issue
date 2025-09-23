package com.ioi.myssue.designsystem.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.navigation.BottomTabRoute
import com.ioi.myssue.navigation.Navigator
import com.ioi.myssue.player.AudioController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopBarViewModel @Inject constructor(
    private val audioController: AudioController,
    private val navigator: Navigator
) : ViewModel() {

    private var _uiState = MutableStateFlow(TopBarUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            audioController.audioState.collect { audio ->
                _uiState.update {
                    it.copy(
                        isConnected = audio.isConnected,
                        isPlaying = audio.isPlaying,
                        playbackState = audio.playbackState
                    )
                }
            }
        }
    }

    fun play() = audioController.play()
    fun pause() = audioController.pause()

    fun navigateToPodcast() = viewModelScope.launch{
        navigator.navigate(BottomTabRoute.Podcast)
    }
}

data class TopBarUiState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val playbackState: Int = 0 // Player.STATE_IDLE, STATE_READY 등
)
