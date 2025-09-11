package com.ioi.myssue.player

import androidx.media3.common.Player

data class AudioState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val duration: Long = 0L,
    val position: Long = 0L,
    val bufferedPosition: Long = 0L,
    val currentIndex: Int = 0,
//    val title: String? = null,
//    val subtitle: String? = null
)
