package com.ioi.myssue.ui.podcast.component.bottomsheetplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.ui.common.clickableNoRipple
import com.ioi.myssue.ui.podcast.component.GradientProgressBar

@Composable
fun BottomControls(
    positionMs: Long,
    durationMs: Long,
    changeDate: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    isPlaying: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 20.dp)) {
        GradientProgressBar(
            positionMs = positionMs,
            durationMs = durationMs,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            gradient = Brush.horizontalGradient(
                listOf(
                    BackgroundColors.Background50, BackgroundColors.Background50
                )
            ),
            onSeekTo = onSeekTo
        )
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            Text(
                text = formatTime(positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = BackgroundColors.Background50
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "-${formatTime((durationMs - positionMs).coerceAtLeast(0))}",
                style = MaterialTheme.typography.labelSmall,
                color = BackgroundColors.Background50
            )
        }
        Spacer(Modifier.height(16.dp))

        BottomControlActionButtons(
            changeDate = changeDate,
            onPlayPause = onPlayPause,
            isPlaying = isPlaying,
            isLoading = isLoading
        )
    }
}

@Composable
private fun BottomControlActionButtons(
    changeDate: (Int) -> Unit,
    onPlayPause: () -> Unit,
    isPlaying: Boolean,
    isLoading: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            painterResource(R.drawable.ic_podcast_previous),
            contentDescription = "previous date",
            tint = BackgroundColors.Background50,
            modifier = Modifier
                .size(32.dp)
                .clickableNoRipple {
                    changeDate(-1)
                }
        )


        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(6.dp).size(36.dp),
                strokeWidth = 4.dp,
                color = BackgroundColors.Background50
            )
        }
        else {
            val icon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_podcast_play
            Icon(
                painter = painterResource(icon),
                contentDescription = "play/pause",
                tint = BackgroundColors.Background50,
                modifier = Modifier
                    .size(48.dp)
                    .clickableNoRipple(onPlayPause)
            )
        }

        Icon(
            painterResource(R.drawable.ic_podcast_next),
            contentDescription = "next date",
            tint = BackgroundColors.Background50,
            modifier = Modifier
                .size(32.dp)
                .clickableNoRipple {
                    changeDate(+1)
                }
        )

    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}