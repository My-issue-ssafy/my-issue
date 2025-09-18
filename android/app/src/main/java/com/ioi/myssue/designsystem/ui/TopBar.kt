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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.BackgroundColors

@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    @DrawableRes logoRes: Int = R.drawable.logo,
    onBellClick: (() -> Unit)? = null,
    onBack : (() -> Unit)? = null,
    containerColor: Color = BackgroundColors.Background50,
) {
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if(onBack != null) {
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
                TopBarMiniPlayer(onBellClick, isPlaying)
            }

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = { onBellClick?.invoke() },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "Notifications",
                    tint = BackgroundColors.Background600,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun TopBarMiniPlayer(onBellClick: (() -> Unit)?, isPlaying: Boolean) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = BackgroundColors.Background100,
    ) {
        IconButton(
            modifier = Modifier.padding(horizontal = 4.dp),
            onClick = { onBellClick?.invoke() },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_radio),
                contentDescription = "Notifications",
                modifier = Modifier.size(32.dp)
            )
        }

        if (isPlaying) {
            IconButton(
                modifier = Modifier.padding(horizontal = 4.dp),
                onClick = { onBellClick?.invoke() },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_radio),
                    contentDescription = "Notifications",
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(
                modifier = Modifier.padding(horizontal = 4.dp),
                onClick = { onBellClick?.invoke() },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_radio),
                    contentDescription = "Notifications",
                    modifier = Modifier.size(32.dp)
                )
            }

        }
    }
}

