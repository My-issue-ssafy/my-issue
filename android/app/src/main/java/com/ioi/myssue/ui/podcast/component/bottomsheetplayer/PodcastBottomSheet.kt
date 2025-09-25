package com.ioi.myssue.ui.podcast.component.bottomsheetplayer

import android.R.attr.onClick
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.designsystem.ui.MyssueBottomSheet
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.domain.model.ScriptLine
import com.ioi.myssue.ui.podcast.KeyWordList
import com.ioi.myssue.ui.podcast.PodcastContentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastBottomSheet(
    onDismissRequest: () -> Unit = {},
    thumbnailUrl: String,
    title: String,
    dateText: String,
    scripts: List<ScriptLine>,
    newsSummaries: List<NewsSummary>,
    keywords: List<String>,
    currentIndex: Int,
    onLineClick: (Int) -> Unit,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    isPlaying: Boolean,
    isLoading: Boolean,
    changeDate: (Int) -> Unit,
    onNewsClick: (Long) -> Unit,
    toggleContentType: () -> Unit,
    contentType: PodcastContentType
) {
    val bottomSheetGradient = Brush.verticalGradient(
        listOf(AppColors.Primary300, AppColors.Primary500)
    )
    var bottomControlsHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    MyssueBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bottomSheetGradient)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomControlsHeight),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                BottomPlayerHeader(
                    thumbnailUrl = thumbnailUrl,
                    title = title,
                    dateText = dateText,
                    toggleContentType = toggleContentType,
                    contentType = contentType
                )

                if (contentType == PodcastContentType.SCRIPT) {
                    ScriptBlockAnimated(
                        scripts = scripts,
                        currentIndex = currentIndex,
                        onLineClick = onLineClick
                    )
                } else if (contentType == PodcastContentType.NEWS) {
                    KeyWordList(
                        keywords = keywords,
                        modifier = Modifier.fillMaxWidth()
                    )

                    NewsSummaryList(
                        news = newsSummaries,
                        onClick = onNewsClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            BottomControls(
                positionMs = positionMs,
                durationMs = durationMs,
                changeDate = changeDate,
                onPlayPause = onPlayPause,
                onSeekTo = onSeekTo,
                isPlaying = isPlaying,
                isLoading = isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .onGloballyPositioned { layoutCoordinates ->
                        bottomControlsHeight =
                            with(density) { layoutCoordinates.size.height.toDp() }
                    }
            )
        }
    }
}
