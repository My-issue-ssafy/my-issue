package com.ioi.myssue.ui.mypage.mytoon

import android.R.attr.end
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ioi.myssue.LocalAnalytics
import com.ioi.myssue.R
import com.ioi.myssue.domain.model.CartoonNews
import com.ioi.myssue.ui.cartoon.CartoonCard
import com.ioi.myssue.ui.cartoon.ExpandedCartoonCard
import com.ioi.myssue.ui.common.clickableNoRipple
import com.ioi.myssue.ui.news.NewsDetail
import com.ioi.myssue.ui.news.NewsSectionHeader

private const val TAG = "MyCartoonScreen"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCartoonScreen(
    viewModel: MyCartoonViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val analytics = LocalAnalytics.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            NewsSectionHeader(
                title = "내가 좋아한 네컷뉴스",
                modifier = Modifier.padding(top = 8.dp),
            )

            TwoColumnGridWithLazyVerticalGrid(
                items = state.myToons,
                onClick = viewModel::setClickedToon
            )
        }

        state.clickedToon?.let {
            BackHandler {
                state.clickedToon?.let {
                    viewModel.setClickedToon(null)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)) // 살짝 어둡게
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        viewModel.setClickedToon(null) // 바깥 클릭 시 닫기
                    },
                contentAlignment = Alignment.Center
            ) {
                ExpandedCartoonCard(
                    cartoon = it,
                    isExiting = false,
                    isFlippable = false,
                    exitDir = 0,
                    modifier = Modifier,
                    onClick = {
                        viewModel.openNewsDetail(it.newsId)
                        analytics.logNewsClick(it.newsId, feedSource = "likedCartoon")
                        Log.d(TAG, "logNewsClick: ${it.newsId} likedCartoon")
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = it.newsTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(end = 20.dp)
                        )


                        Image(
                            painter = if (it.isLiked) painterResource(R.drawable.ic_heart)
                            else painterResource(R.drawable.ic_no_heart),
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clickableNoRipple {
                                    viewModel.toggleLike(it)
                                }
                        )

                    }

                }
            }
        }
    }

    state.selectedNewsId?.let {
        NewsDetail(
            newsId = it,
            sheetState = sheetState,
            onDismiss = { viewModel.closeNewsDetail() }
        )
    }
}

@Composable
fun TwoColumnGridWithLazyVerticalGrid(
    items: List<CartoonNews>,
    onClick: (CartoonNews) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // 2열 고정
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items) { item ->
            CartoonCard(
                cartoon = item,
                isExiting = false,
                exitDir = 0,
                isFlippable = false,
                onClick = { onClick(item) },
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}
