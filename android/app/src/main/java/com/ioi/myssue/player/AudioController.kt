package com.ioi.myssue.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.map
import androidx.core.net.toUri

class AudioController(
    private val appContext: Context
) {
    private var controller: MediaController? = null
    private var scope: CoroutineScope? = null
    private var positionTickerJob: Job? = null

    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState

    private fun updateState(block: AudioState.() -> AudioState) {
        _audioState.value = _audioState.value.block()
    }

    private val listener = @UnstableApi
    object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState { copy(isPlaying = isPlaying) }
            managePositionTicker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val ctl = controller ?: return
            updateState {
                copy(
                    playbackState = playbackState,
                    duration = maxOf(0L, ctl.duration),
                    bufferedPosition = ctl.bufferedPosition
                )
            }
        }

        override fun onPositionDiscontinuity(reason: Int) {
            val ctl = controller ?: return
            updateState {
                copy(
                    position = maxOf(0L, ctl.currentPosition),
                    bufferedPosition = ctl.bufferedPosition
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val ctl = controller ?: return
            updateState {
                copy(
                    currentIndex = ctl.currentMediaItemIndex,
                    duration = maxOf(0L, ctl.duration),
                    position = maxOf(0L, ctl.currentPosition),
                    bufferedPosition = ctl.bufferedPosition
                )
            }
            updateMetadata(mediaItem?.mediaMetadata)
        }

        override fun onEvents(player: Player, events: Player.Events) {
            // 디바이스마다 콜백 편차 보강
            updateState { copy(bufferedPosition = player.bufferedPosition) }
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun connect(): MediaController = withContext(Dispatchers.Main) {
        controller?.let { return@withContext it }

        val token = SessionToken(appContext, ComponentName(appContext, AudioService::class.java))
        val ctl = MediaController.Builder(appContext, token).buildAsync().await()
        controller = ctl
        scope = CoroutineScope(Dispatchers.Main.immediate + Job())

        // 초기 스냅샷
        updateState {
            copy(
                isConnected = true,
                isPlaying = ctl.isPlaying,
                playbackState = ctl.playbackState,
                currentIndex = ctl.currentMediaItemIndex,
                duration = maxOf(0L, ctl.duration),
                position = maxOf(0L, ctl.currentPosition),
                bufferedPosition = ctl.bufferedPosition,
                title = ctl.mediaMetadata.title?.toString(),
                subtitle = ctl.mediaMetadata.artist?.toString()
            )
        }

        ctl.addListener(listener)
        managePositionTicker()
        ctl
    }

    fun release() {
        positionTickerJob?.cancel()
        positionTickerJob = null

        controller?.removeListener(listener)
        controller?.release()
        controller = null

        scope?.cancel()
        scope = null

        updateState {
            AudioState() // 기본값으로 리셋
        }
    }

    // ---------- Commands ----------
    fun setPlaylist(items: List<Triple<Uri, String, String>>) {
        val mediaItems = items.map { (uri, title, thumbnail) ->
            MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist("HOT 뉴스")
                        .setArtworkUri(thumbnail.toUri())
                        .build()
                )
                .build()
        }

        controller?.setMediaItems(mediaItems)
        controller?.prepare()
    }


    fun play() = controller?.play()
    fun pause() = controller?.pause()
    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun stop() {
        controller?.pause()
        controller?.seekTo(0)
    }

    fun next() = controller?.seekToNextMediaItem()
    fun prev() = controller?.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        controller?.let { ctl ->
            updateState {
                copy(
                    position = maxOf(0L, ctl.currentPosition),
                    bufferedPosition = ctl.bufferedPosition
                )
            }
        }
    }

    private fun updateMetadata(md: MediaMetadata?) {
        updateState {
            copy(
                title = md?.title?.toString(),
                subtitle = md?.artist?.toString()
            )
        }
    }

    /**
     * position은 프레임 콜백이 없으므로 재생 중에만 250ms 주기로 가볍게 갱신.
     * (isPlaying/transition 등은 이벤트 기반)
     */
    private fun managePositionTicker() {
        val ctl = controller ?: return
        val playing = ctl.isPlaying

        if (playing && positionTickerJob?.isActive != true) {
            positionTickerJob = scope?.launch {
                while (isActive && controller?.isPlaying == true) {
                    val c = controller ?: break
                    updateState {
                        copy(
                            position = maxOf(0L, c.currentPosition),
                            bufferedPosition = c.bufferedPosition
                        )
                    }
                    delay(250)
                }
            }
        } else if (!playing) {
            positionTickerJob?.cancel()
            positionTickerJob = null
        }
    }
}
