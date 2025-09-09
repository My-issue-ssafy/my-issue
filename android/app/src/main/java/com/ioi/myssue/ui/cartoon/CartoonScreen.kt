package com.ioi.myssue.ui.cartoon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ioi.myssue.R

@Composable
fun CartoonScreen(
    viewModel: CartoonViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator()
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
                    Text(text = "뉴스 충전 중...", style = MaterialTheme.typography.titleLarge)
                    Text(text= "내일의 네컷뉴스를 기다려주세요!", style = MaterialTheme.typography.titleLarge)
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
            kotlinx.coroutines.delay(frameDurationMillis)
            idx = (idx + 1) % frames.size
        }
    }

    Image(
        painter = painterResource(frames[idx]),
        contentDescription = null,
        modifier = modifier
    )
}
