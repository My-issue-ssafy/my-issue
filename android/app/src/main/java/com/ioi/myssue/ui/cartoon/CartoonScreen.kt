package com.ioi.myssue.ui.cartoon

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ioi.myssue.LocalAnalytics
import com.ioi.myssue.R
import com.ioi.myssue.ui.news.NewsDetail
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartoonScreen(
    viewModel: CartoonViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var showingNewsId by remember { mutableLongStateOf(-1L) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onExitFinished()
        }
    }

    val anayltics = LocalAnalytics.current

    LaunchedEffect(uiState.currentCartoonIndex) {
        if(uiState.currentCartoonIndex >= 0) {
            uiState.currentToonId?.let {
                Log.d("CartoonScreen", "GA logToonImpression toonId: $it")
                anayltics.logToonImpression(it)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator()
            }

            uiState.error != null -> {
                Image(
                    painter = painterResource(R.drawable.ic_empty_toon),
                    contentDescription = null,
                )

                Text(
                    text = uiState.error ?: "",
                    style = MaterialTheme.typography.titleLarge.copy(lineHeight = 40.sp),
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center
                )
            }

            uiState.isEmpty -> {
                Image(
                    painter = painterResource(R.drawable.ic_empty_toon),
                    contentDescription = null,
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BatteryChargingIcon()
                    Text(
                        text = stringResource(R.string.charging_news),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.empty_cartoon_news_description),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CartoonCardStack(
                        cartoonList = uiState.cartoonNewsList,
                        currentIndex = uiState.currentCartoonIndex,
                        exitTrigger = uiState.exitTrigger,
                        isSwiping = uiState.isSwiping,
                        onExitFinished = viewModel::onExitFinished,
                        onLikePressed = viewModel::onLikePressed,
                        onHatePressed = viewModel::onHatePressed,
                        onShowDetail = {
                            Log.d("CartoonScreen", "it: $it")
                            showingNewsId = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    CartoonActionButtons(
                        isLikePressed = uiState.isLikePressed,
                        isHatePressed = uiState.isHatePressed,
                        canInteract = uiState.canInteract(),
                        onLikePressed = viewModel::onLikePressed,
                        onHatePressed = viewModel::onHatePressed
                    )
                }

                Log.d("CartoonScreen", "showingNewsId: $showingNewsId")
                if(showingNewsId>=0) {
                    NewsDetail(
                        newsId = showingNewsId,
                        sheetState = sheetState,
                        onDismiss = {showingNewsId = -1L},
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryChargingIcon(
    modifier: Modifier = Modifier,
    frameDurationMillis: Long = 300L,
    isRunning: Boolean = true
) {
    val frames = remember {
        listOf(
            R.drawable.ic_battery1,
            R.drawable.ic_battery2,
            R.drawable.ic_battery3,
            R.drawable.ic_battery4,
            R.drawable.ic_battery5,
            R.drawable.ic_battery6
        )
    }
    var idx by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRunning, frameDurationMillis, frames) {
        if (!isRunning || frames.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(frameDurationMillis)
            idx = (idx + 1) % frames.size
        }
    }

    Image(
        painter = painterResource(frames[idx]),
        contentDescription = null,
        modifier = modifier
    )
}
