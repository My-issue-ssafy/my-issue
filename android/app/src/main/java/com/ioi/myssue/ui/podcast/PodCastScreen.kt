package com.ioi.myssue.ui.podcast

import android.R.id.toggle
import android.annotation.SuppressLint
import android.media.session.PlaybackState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.ui.common.clickableNoRipple
import com.ioi.myssue.ui.news.NewsDetail
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
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (state.audio.playbackState != PlaybackState.STATE_PAUSED &&
                        state.audio.playbackState != PlaybackState.STATE_PLAYING) {
                        viewModel.selectDate(playNow = false)
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box {
        Column(Modifier.fillMaxSize()) {
            Calendar(
                isMonthlyView = state.isMonthlyView,
                selectedDate = state.selectedDate,
                onToggleView = { viewModel.toggleCalendarViewType() },
                onDateSelected = { date -> viewModel.selectDate(date) },
                modifier = Modifier.padding(16.dp)
            )

            Spacer(Modifier.height(12.dp))

            if (state.isMonthlyView) {
                Spacer(Modifier.weight(1f))
                BottomPlayer(
                    title = "HOT 뉴스",
                    date = state.selectedDateString,
                    thumbnail = state.episode.thumbnail,
                    isPlaying = state.audio.isPlaying,
                    keywords = state.episode.keyWords,
                    onPlayPause = viewModel::toggle,
                    openBottomPlayer = viewModel::openPlayer
                )
            } else {
                PlayerContent(
                    title = state.selectedDateString,
                    imageUrl = state.episode.thumbnail,
                    isPlaying = state.audio.isPlaying,
                    isLoading = state.isLoading,
                    positionMs = state.audio.position,
                    durationMs = state.audio.duration,
                    currentLine = state.currentLine.line,
                    previousLine = state.previousLine.line,
                    onPlayPause = { viewModel.toggle() },
                    onSeekTo = { viewModel.seekTo(it) },
                    openBottomPlayer = viewModel::openPlayer
                )

            }
        }

        if (state.showPlayer) {
            PodcastBottomSheet(
                thumbnailUrl = state.episode.thumbnail,
                title = "HOT 뉴스",
                dateText = state.selectedDateString,
                positionMs = state.audio.position,
                durationMs = state.audio.duration,
                onPlayPause = viewModel::toggle,
                onSeekTo = viewModel::seekTo,
                isPlaying = state.audio.isPlaying,
                isLoading = state.isLoading,
                changeDate = viewModel::changeDate,
                onDismissRequest = viewModel::closePlayer,
                scripts = state.episode.scripts,
                keywords = state.episode.keyWords,
                newsSummaries = state.podcastNewsSummaries,
                currentIndex = state.currentScriptIndex,
                onLineClick = viewModel::updateIndex,
                toggleContentType = viewModel::toggleContentType,
                onNewsClick = viewModel::openDetail,
                contentType = state.contentType
            )
        }
        state.detailNewsId?.let {
            val sheetState = rememberModalBottomSheetState(true)

            NewsDetail(newsId = it, sheetState = sheetState, onDismiss = viewModel::closeDetail)
        }
    }

}

@Composable
private fun BottomPlayer(
    title: String,
    date: String,
    thumbnail: String,
    keywords: List<String>,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    openBottomPlayer: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
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
                .fillMaxWidth()
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

            Spacer(Modifier.height(24.dp))

            KeyWordList(
                keywords = keywords,
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(4.dp))
        }
    }
}
@Composable
fun KeyWordList(
    keywords: List<String>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.horizontalScroll(rememberScrollState()).widthIn(max = 550.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxLines = 3
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
}



@SuppressLint("CoroutineCreationDuringComposition")
@Composable
private fun ColumnScope.PlayerContent(
    title: String,
    imageUrl: String,
    isPlaying: Boolean,
    isLoading: Boolean,
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
        isLoading = isLoading,
        positionMs = positionMs,
        durationMs = durationMs,
        onSeekTo = onSeekTo,
        onPlayPause = onPlayPause,
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 16.dp)
            .clickableNoRipple{openBottomPlayer()}
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
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 40.dp)
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
                color = BackgroundColors.Background50,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
