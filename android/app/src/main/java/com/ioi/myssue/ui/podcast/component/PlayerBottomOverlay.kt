package com.ioi.myssue.ui.podcast.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.designsystem.theme.BackgroundColors

@Composable
fun PlayerBottomOverlay(
    title: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        GradientProgressBar(
            positionMs = positionMs,
            durationMs = durationMs,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = BackgroundColors.Background50,
                modifier = Modifier.padding(horizontal = 8.dp),
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onPlayPause) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_radio_play
                    ),
                    contentDescription = null,
                    tint = BackgroundColors.Background50
                )
            }
        }
    }
}

@Composable
fun GradientProgressBar(
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
    gradient: Brush = Brush.horizontalGradient(
        listOf(AppColors.Primary300, AppColors.Primary500)
    )
) {
    val clampedDur = durationMs.coerceAtLeast(0L)
    val fraction = if (clampedDur > 0) {
        positionMs.coerceIn(0, clampedDur).toFloat() / clampedDur
    } else 0f

    Box(
        modifier = modifier
            .height(6.dp)
            .background(BackgroundColors.Background100.copy(alpha = 0.6f))
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(brush = gradient, shape = CircleShape)
        )
    }
}
