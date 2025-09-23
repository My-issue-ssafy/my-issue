package com.ioi.myssue.designsystem.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.player.AudioController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopBarViewModel @Inject constructor(
    private val audioController: AudioController
) : ViewModel() {

    init {
        viewModelScope.launch {
            audioController.audioState.collect { audio ->
                if (audio.isPlaying) {
                }
            }
        }

    }

    fun play() = audioController.play()
    fun pause() = audioController.pause()
}
