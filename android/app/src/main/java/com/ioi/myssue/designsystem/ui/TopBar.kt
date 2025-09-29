package com.ioi.myssue.designsystem.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.ui.common.clickableNoRipple

@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    @DrawableRes logoRes: Int = R.drawable.logo,
    onBellClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    containerColor: Color = BackgroundColors.Background50,
    mode: TopBarMode = TopBarMode.Default,
    notificationEnabled: Boolean? = null,
    onToggleNotification: (Boolean) -> Unit = { },
    hasUnread: Boolean = false,
    topBarViewModel: TopBarViewModel = hiltViewModel()
) {
    val uiState by topBarViewModel.uiState.collectAsState()

    val topBarInsets = WindowInsets
        .safeDrawing
        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .windowInsetsPadding(topBarInsets)
            .padding(8.dp)
    ) {
        Image(
            painter = painterResource(logoRes),
            contentDescription = "App logo",
            modifier = Modifier
                .size(48.dp)
                .padding(4.dp)
                .align(Alignment.Center)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = { onBack.invoke() },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back_normal),
                        contentDescription = "Back",
                        tint = BackgroundColors.Background600,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                TopBarMiniPlayer(
                    uiState = uiState,
                    onPlay = topBarViewModel::play,
                    onPause = topBarViewModel::pause,
                    onRadioClick = topBarViewModel::navigateToPodcast
                )
            }

            Spacer(Modifier.weight(1f))

            when (mode) {
                TopBarMode.Default -> {
                    IconButton(onClick = { onBellClick?.invoke() }) {
                        Image(
                            painter = painterResource(
                                if (hasUnread) R.drawable.ic_notification_new
                                else R.drawable.ic_notification
                            ),
                            contentDescription = if (hasUnread) "읽지 않은 알림 있음" else "알림",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                TopBarMode.Notification -> {
                    if (notificationEnabled != null) {

                        Image(
                            painter = painterResource(
                                if (notificationEnabled) R.drawable.ic_notification_on
                                else R.drawable.ic_notification_off
                            ),
                            contentDescription = if (notificationEnabled) "알림 켜짐" else "알림 꺼짐",
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .width(76.dp)
                                .clickableNoRipple {
                                    onToggleNotification(!notificationEnabled)
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarMiniPlayer(
    uiState: TopBarUiState,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onRadioClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = BackgroundColors.Background100,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRadioClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_radio),
                    contentDescription = "Radio",
                    modifier = Modifier.size(28.dp)
                )
            }

            when {
                !uiState.isConnected || uiState.playbackState != androidx.media3.common.Player.STATE_READY -> {}

                uiState.isPlaying -> {
                    IconButton(onClick = onPause) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pause),
                            contentDescription = "Pause",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                else -> {
                    IconButton(onClick = onPlay) {
                        Icon(
                            painter = painterResource(R.drawable.ic_radio_play),
                            contentDescription = "Play",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

        }
    }
}

