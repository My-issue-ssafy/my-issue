package com.ioi.myssue.player

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager

@UnstableApi
class AudioService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private var notificationManager: PlayerNotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()

        createChannel()

        notificationManager = PlayerNotificationManager.Builder(
            this,
            NOTI_ID,
            CHANNEL_ID
        )
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player) =
                    player.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Unknown"

                override fun createCurrentContentIntent(player: Player) = mediaSession.sessionActivity

                override fun getCurrentContentText(player: Player) =
                    player.currentMediaItem?.mediaMetadata?.artist?.toString() ?: ""

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ) = null
            })
            .setChannelImportance(NotificationManager.IMPORTANCE_LOW)
            .setSmallIconResourceId(android.R.drawable.ic_media_play)
            .build().apply {
                setPlayer(player)
            }

        val noti = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Audio")
            .setContentText("Loading...")
            .build()
        startForeground(NOTI_ID, noti)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        notificationManager?.setPlayer(null)
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(
            CHANNEL_ID,
            "Audio Playback",
            NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(ch)
    }

    companion object {
        private const val CHANNEL_ID = "audio_playback"
        private const val NOTI_ID = 1001
    }
}
