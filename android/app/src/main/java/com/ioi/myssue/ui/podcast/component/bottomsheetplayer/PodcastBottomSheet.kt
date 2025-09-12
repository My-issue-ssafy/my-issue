package com.ioi.myssue.ui.podcast.component.bottomsheetplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.ioi.myssue.designsystem.theme.AppColors
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
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bottomSheetContainerColor = AppColors.Primary300
    val bottomSheetGradient = Brush.verticalGradient(
        listOf(AppColors.Primary300, AppColors.Primary500)
    )

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
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                BottomPlayerHeader(
                    thumbnailUrl = thumbnailUrl,
                    title = title,
                    dateText = dateText
                )

                ScriptBlockAnimated(
                    scripts = scripts,
                    currentIndex = currentIndex
                )

                BottomControls(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    changeDate = changeDate,
                    onPlayPause = onPlayPause,
                    isPlaying = isPlaying
                )
            }
        }
    }
}

