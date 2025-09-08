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
import androidx.compose.runtime.getValue
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
                    Image(painter = painterResource(R.drawable.ic_battery1), contentDescription = null)
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
                        onExitFinished = { viewModel.onExitFinished() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    CartoonActionButtons(
                        isLikePressed = uiState.isLikePressed,
                        isHatePressed = uiState.isHatePressed,
                        canInteract = uiState.canInteract(),
                        onLikePressed = { viewModel.onLikePressed() },
                        onHatePressed = { viewModel.onHatePressed() }
                    )
                }
            }
        }
    }
}