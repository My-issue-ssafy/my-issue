package com.ioi.myssue.ui.podcast

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.player.AudioController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val audioController: AudioController
) : ViewModel() {

    private val _state = MutableStateFlow(PodcastUiState())
    val state: StateFlow<PodcastUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            audioController.audioState.collect { audio ->
                val episode = _state.value.episode

                _state.value = _state.value.copy(audio = audio)

                if (audio.isPlaying) {
                    val line = episode.scripts.lastOrNull { it.startMs <= audio.position }
                    if (line != null && line.text != _state.value.currentLine) {
                        _state.value = _state.value.copy(
                            previousLine = _state.value.currentLine,
                            currentLine = line.text
                        )
                    }
                }
            }
        }

        selectDate(LocalDate.now())
    }

    fun toggleViewMode() {
        _state.value = _state.value.copy(
            isMonthlyView = !_state.value.isMonthlyView
        )
    }

    fun selectDate(date: LocalDate) {
        val episode = dummyEpisodes.random()

        _state.value = _state.value.copy(
            selectedDate = date,
            episode = episode,
            currentLine = "",
            previousLine = ""
        )

        viewModelScope.launch {
            audioController.connect()
            audioController.setPlaylist(listOf(episode.audioUrl.toUri()))
            audioController.play()
        }
    }

    fun play() = audioController.play()
    fun pause() = audioController.pause()
    fun toggle() = audioController.togglePlayPause()
    fun next() = audioController.next()
    fun prev() = audioController.prev()
    fun seekTo(ms: Long) = audioController.seekTo(ms)

    override fun onCleared() {
        audioController.release()
        super.onCleared()
    }
}
