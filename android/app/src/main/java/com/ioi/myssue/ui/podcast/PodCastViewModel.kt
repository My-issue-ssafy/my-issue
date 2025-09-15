package com.ioi.myssue.ui.podcast

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.player.AudioController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                    val idx = episode.scripts.indexOfLast { it.startMs <= audio.position }
                    if (idx >= 0 && idx != _state.value.currentIndex) {
                        val current = episode.scripts[idx]
                        val prev = if (idx > 0) episode.scripts[idx - 1] else ScriptLine()
                        _state.value = _state.value.copy(
                            currentIndex = idx,
                            currentLine = current,
                            previousLine = prev
                        )
                    }
                }
            }
        }
        selectDate(LocalDate.now())
    }

    fun toggleCalendarViewType() {
        _state.value = _state.value.copy(
            isMonthlyView = !_state.value.isMonthlyView
        )
    }

    fun toggleContentType() {
        val toggledContentType = when(_state.value.contentType) {
            PodcastContentType.SCRIPT -> PodcastContentType.NEWS
            PodcastContentType.NEWS -> PodcastContentType.SCRIPT
        }
        _state.value = _state.value.copy(contentType = toggledContentType)
    }

    fun selectDate(date: LocalDate) {
        val episode = dummyEpisodes.random()
        _state.value = _state.value.copy(
            selectedDate = date,
            episode = episode,
            currentLine = ScriptLine(),
            previousLine = ScriptLine(),
            currentIndex = -1
        )

        viewModelScope.launch {
            audioController.connect()
            audioController.setPlaylist(listOf(episode.audioUrl.toUri()))
            audioController.play()
        }
    }

    fun openPlayer() {
        _state.value = _state.value.copy(showPlayer = true)
    }

    fun closePlayer() {
        _state.value = _state.value.copy(showPlayer = false)
    }

    fun play() = audioController.play()
    fun pause() = audioController.pause()
    fun toggle() = audioController.togglePlayPause()
    fun next() = audioController.next()
    fun prev() = audioController.prev()
    fun seekTo(ms: Long) = audioController.seekTo(ms)

    fun changeDate(step: Int) {
        if(step < 0 && _state.value.currentIndex > 0) {
            seekTo(0L)
            return
        }
        selectDate(_state.value.selectedDate.plusDays(step.toLong()))
    }

    override fun onCleared() {
        audioController.release()
        super.onCleared()
    }
}
