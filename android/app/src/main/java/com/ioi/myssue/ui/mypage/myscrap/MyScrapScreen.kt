package com.ioi.myssue.ui.mypage.myscrap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.ui.news.NewsDetail
import com.ioi.myssue.ui.news.NewsItem
import com.ioi.myssue.ui.news.NewsSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScrapScreen(
    viewModel: MyScrapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColors.Background100)
    ) {
        NewsSectionHeader("내가 스크랩한 뉴스")
        LazyColumn {
            items(state.newsSummaries) { item ->
                NewsItem(
                    modifier = Modifier,
                    news = item,
                    isMarked = true,
                    onClick = { viewModel.openNewsDetail(item.newsId) }
                )
            }
        }
    }

    if(state.selectedNewsId != null) {
        NewsDetail(
            newsId = state.selectedNewsId,
            sheetState = sheetState,
            onDismiss = { viewModel.closeNewsDetail() }
        )
    }
}