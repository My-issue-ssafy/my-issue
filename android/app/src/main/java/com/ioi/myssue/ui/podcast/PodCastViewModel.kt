package com.ioi.myssue.ui.podcast

import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.domain.model.PodcastEpisode
import com.ioi.myssue.domain.model.ScriptLine
import com.ioi.myssue.domain.repository.PodcastRepository
import com.ioi.myssue.player.AudioController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val audioController: AudioController
) : ViewModel() {

    sealed interface PodcastEvent {
        data class UpdateFgNotification(val episode: PodcastEpisode) : PodcastEvent
    }

    private val _state = MutableStateFlow(PodCastUiState())
    val state: StateFlow<PodCastUiState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<PodcastEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            audioController.audioState.collect { audio ->
                val episode = _state.value.episode
                _state.value = _state.value.copy(audio = audio)

                if (audio.isPlaying) {
                    val idx = episode.scripts.indexOfLast { it.startMs <= audio.position }
                    if (idx >= 0 && idx != _state.value.currentScriptIndex) {
                        val current = episode.scripts[idx]
                        val prev = if (idx > 0) episode.scripts[idx - 1] else ScriptLine()
                        _state.value = _state.value.copy(
                            currentScriptIndex = idx,
                            currentLine = current,
                            previousLine = prev
                        )
                    }
                }
            }
        }
        selectDate(LocalDate.now(), false)
    }

    fun toggleCalendarViewType() {
        _state.value = _state.value.copy(
            isMonthlyView = !_state.value.isMonthlyView
        )
    }

    fun toggleContentType() {
        val toggledContentType = when (_state.value.contentType) {
            PodcastContentType.SCRIPT -> PodcastContentType.NEWS
            PodcastContentType.NEWS -> PodcastContentType.SCRIPT
        }
        _state.update { it.copy(contentType = toggledContentType) }
    }

    fun selectDate(date: LocalDate = _state.value.selectedDate, playNow: Boolean = true) = viewModelScope.launch {
        audioController.stop()
        audioController.release()
        _state.update {
            it.copy(
                selectedDate = date,
                episode = PodcastEpisode(),
                currentLine = ScriptLine(),
                previousLine = ScriptLine(),
                currentScriptIndex = -1
            )
        }

        runCatching {
            podcastRepository.getPodcast(
                date.format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.KOREA)
                )
            )
        }.onSuccess { podcastEpisode ->
            _state.update { it.copy(episode = podcastEpisode) }

            audioController.connect()
            audioController.setPlaylist(
                listOf(
                    Triple(
                        podcastEpisode.podcastUrl.toUri(),
                        formatNewsDate(date),
                        podcastEpisode.thumbnail
                    )
                )
            )
            if (playNow) {
                audioController.play()
            }
            _event.emit(PodcastEvent.UpdateFgNotification(_state.value.episode))
        }.onFailure { error ->
            Log.d("PodcastViewModel", error.toString())
        }

        runCatching {
            podcastRepository.getNewsByPodcast(_state.value.episode.podcastId)
        }.onSuccess { newsSummaries ->
            _state.update {
                it.copy(
                    podcastNewsSummaries = newsSummaries
                )
            }
        }
    }

    fun updateIndex(index: Int) = viewModelScope.launch {
        if (index < 0 || index >= _state.value.episode.scripts.size) return@launch
        if (_state.value.isLoading) return@launch

        _state.update { it.copy(currentScriptIndex = index) }
        seekTo(_state.value.episode.scripts[index].startMs)
        play()
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
        if (step < 0 && _state.value.currentScriptIndex > 0) {
            seekTo(0L)
            return
        }
        selectDate(_state.value.selectedDate.plusDays(step.toLong()))
    }

    private fun formatNewsDate(dateStr: LocalDate): String {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.KOREA)
        val outputFormatter = DateTimeFormatter.ofPattern("MM월 dd일", Locale.KOREA)

        val date = LocalDate.parse(dateStr.toString(), inputFormatter)
        return "${date.format(outputFormatter)} 뉴스 요약"
    }

    override fun onCleared() {
        audioController.release()
        super.onCleared()
    }
}
