package com.ioi.myssue.ui.podcast

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.ui.podcast.component.Calendar
import com.ioi.myssue.ui.podcast.component.CurvedSurface
import com.ioi.myssue.ui.podcast.component.MiniPlayer
import com.ioi.myssue.ui.podcast.component.PlayerCard
import com.ioi.myssue.ui.podcast.component.bottomsheetplayer.PodcastBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodCastScreen(
    viewModel: PodcastViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box {
        Column(Modifier.fillMaxSize()) {
            Calendar(
                isMonthlyView = state.isMonthlyView,
                selectedDate = state.selectedDate,
                onToggleView = { viewModel.toggleViewMode() },
                onDateSelected = { date -> viewModel.selectDate(date) },
                modifier = Modifier.padding(16.dp)
            )

            Spacer(Modifier.height(20.dp))

            if (state.isMonthlyView) {
                BottomPlayer(
                    title = "HOT 뉴스",
                    date = state.selectedDateString,
                    thumbnail = state.episode.articleImage,
                    isPlaying = state.audio.isPlaying,
                    onPlayPause = viewModel::toggle,
                    openBottomPlayer = viewModel::openPlayer
                )
            } else {
                PlayerContent(
                    title = state.selectedDateString,
                    imageUrl = state.episode.articleImage,
                    isPlaying = state.audio.isPlaying,
                    positionMs = state.audio.position,
                    durationMs = state.audio.duration,
                    currentLine = state.currentLine.text,
                    previousLine = state.previousLine.text,
                    onPlayPause = { viewModel.toggle() },
                    onSeekTo = { viewModel.seekTo(it) },
                    openBottomPlayer = viewModel::openPlayer
                )

            }
        }

        if (state.showPlayer) {
            PodcastBottomSheet(
                thumbnailUrl = state.episode.articleImage,
                title = "HOT 뉴스",
                dateText = state.selectedDateString,
                positionMs = state.audio.position,
                durationMs = state.audio.duration,
                onPlayPause = viewModel::toggle,
                isPlaying = state.audio.isPlaying,
                changeDate = viewModel::changeDate,
                onDismissRequest = viewModel::closePlayer,
                scripts = state.episode.scripts,
                currentIndex = state.currentIndex
            )
        }
    }

}

@Composable
private fun BottomPlayer(
    title: String,
    date: String,
    thumbnail: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    openBottomPlayer: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppColors.Primary400,
                        AppColors.Primary500
                    )
                ),
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
            ),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MiniPlayer(
                title = title,
                date = date,
                isPlaying = isPlaying,
                thumbnail = thumbnail,
                onClickPlayPause = onPlayPause,
                openBottomPlayer = openBottomPlayer,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(32.dp))

            KeyWordList(
                keywords = listOf("경제", "부동산", "주식", "코인", "금리", "대출", "경제", "부동산", "주식", "코인"),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun KeyWordList(
    keywords: List<String>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        keywords.forEach { keyword ->
            Box(
                modifier = Modifier
                    .background(
                        color = AppColors.Primary50.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = keyword,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BackgroundColors.Background50
                )
            }
        }
    }
}


@SuppressLint("CoroutineCreationDuringComposition")
@Composable
private fun ColumnScope.PlayerContent(
    title: String,
    imageUrl: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    currentLine: String,
    previousLine: String,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    openBottomPlayer: () -> Unit
) {
    PlayerCard(
        title = title,
        imageUrl = imageUrl,
        isPlaying = isPlaying,
        positionMs = positionMs,
        durationMs = durationMs,
        onSeekTo = onSeekTo,
        onPlayPause = onPlayPause,
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 16.dp)
    )

    Spacer(Modifier.height(20.dp))

    CurvedSurface {
        ScriptViewer(currentLine, previousLine, openBottomPlayer)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ScriptViewer(
    currentLine: String,
    previousLine: String,
    openBottomPlayer: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clickable(
                indication = null,
                interactionSource = interactionSource,
            ) { openBottomPlayer() },
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = previousLine,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith
                        (slideOutVertically { -it / 2 } + fadeOut())
            },
            label = "prevLine"
        ) { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.Primary50,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(8.dp))

        AnimatedContent(
            targetState = currentLine,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith
                        (slideOutVertically { -it / 2 } + fadeOut())
            },
            label = "currentLine"
        ) { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BackgroundColors.Background50
            )
        }
    }
}
