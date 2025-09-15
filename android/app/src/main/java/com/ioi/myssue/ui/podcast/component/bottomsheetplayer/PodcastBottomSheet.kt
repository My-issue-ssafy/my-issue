package com.ioi.myssue.ui.podcast.component.bottomsheetplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.ui.podcast.KeyWordList
import com.ioi.myssue.ui.podcast.PodcastContentType
import com.ioi.myssue.ui.podcast.ScriptLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastBottomSheet(
    onDismissRequest: () -> Unit = {},
    thumbnailUrl: String,
    title: String,
    dateText: String,
    scripts: List<ScriptLine>,
    currentIndex: Int,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    isPlaying: Boolean,
    changeDate: (Int) -> Unit,
    toggleContentType: () -> Unit,
    contentType: PodcastContentType
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val bottomSheetContainerColor = AppColors.Primary300
    val bottomSheetGradient = Brush.verticalGradient(
        listOf(AppColors.Primary300, AppColors.Primary500)
    )
    var bottomControlsHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 55.dp, topEnd = 55.dp),
        containerColor = bottomSheetContainerColor,
        modifier = Modifier.systemBarsPadding()
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
                    toggleContentType = toggleContentType
                )

                if (contentType == PodcastContentType.SCRIPT) {
                    ScriptBlockAnimated(
                        scripts = scripts,
                        currentIndex = currentIndex
                    )
                } else if (contentType == PodcastContentType.NEWS) {
                    PodcastNewsList(
                        news = listOf("", "", "", "", "", "", "", "", ""),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            BottomControls(
                positionMs = positionMs,
                durationMs = durationMs,
                changeDate = changeDate,
                onPlayPause = onPlayPause,
                isPlaying = isPlaying,
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
