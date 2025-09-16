package com.ioi.myssue.ui.news

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.domain.model.NewsSummary
import kotlinx.coroutines.launch

val NewsFeedType.title: String
    @Composable get() = stringResource(titleRes)

// 뉴스 메인 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    viewModel: NewsMainViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // 뉴스 로딩
    if (uiState.isInitialLoading) {
        FullscreenLoading()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp)
    ) {
        NewsHOT(
            list = uiState.main.hot,
            onItemClick = viewModel::onItemClick,
            onAllClick = { viewModel.onClickSeeAll(NewsFeedType.HOT) }
        )
        NewsRecommend(
            list = uiState.main.recommend,
            onItemClick = viewModel::onItemClick,
            onAllClick = { viewModel.onClickSeeAll(NewsFeedType.RECOMMEND) }
        )
        NewsRecent(
            list = uiState.main.latest,
            onItemClick = viewModel::onItemClick,
            onAllClick = { viewModel.onClickSeeAll(NewsFeedType.LATEST) }
        )
    }


    // 바텀시트
    uiState.selectedId?.let { id ->
        NewsDetail(
            newsId = id,
            sheetState = sheetState,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    viewModel.onItemClose()
                }
            }
        )
    }
}

// 섹션별 전체보기 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsAllScreen(
    type: NewsFeedType,
    viewModel: NewsAllViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(type) {
        when (type) {
            NewsFeedType.HOT -> viewModel.getAllHotNews()
            NewsFeedType.RECOMMEND -> viewModel.getAllRecommendNews()
            NewsFeedType.LATEST -> viewModel.getAllRecentNews()
        }
    }

    if (uiState.isInitialLoading) {
        FullscreenLoading()
        return
    }

    // 바닥에서 스크롤 시 다음 뉴스 로딩
    val listState = rememberLazyListState()
    val loadMore by remember(uiState.items.size, uiState.nextCursor) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= uiState.items.size - 5 && uiState.nextCursor != null
        }
    }
    LaunchedEffect(loadMore) {
        if (loadMore) {
            when (type) {
                NewsFeedType.HOT -> viewModel.getAllHotNews(uiState.nextCursor)
                NewsFeedType.RECOMMEND -> viewModel.getAllRecommendNews(uiState.nextCursor)
                NewsFeedType.LATEST -> viewModel.getAllRecentNews(uiState.nextCursor)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColors.Background50),
        contentPadding = PaddingValues(4.dp)
    ) {
        item { NewsSectionHeader(type.title, onAllClick = null) }
        NewsAll(
            list = uiState.items,
            onItemClick = viewModel::onItemClick,
        )
    }

    // 바텀시트
    uiState.selectedId?.let { id ->
        NewsDetail(
            newsId = id,
            sheetState = sheetState,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    viewModel.onItemClose()
                }
            }
        )
    }
}

fun LazyListScope.NewsHOT(
    list: List<NewsSummary>,
    onItemClick: (Long) -> Unit,
    onAllClick: () -> Unit,
) {
    item {
        NewsSectionHeader(
            stringResource(R.string.news_feed_type_hot),
            onAllClick = onAllClick
        )
    }
    if (list.isEmpty()) {
        item {
            NewsEmpty()
        }
    } else {
        item {
            HotNewsPager(
                items = list,
                onClick = { news -> onItemClick(news.newsId) }
            )
        }
    }
}

fun LazyListScope.NewsRecommend(
    list: List<NewsSummary>,
    onItemClick: (Long) -> Unit,
    onAllClick: () -> Unit,
) {
    item {
        NewsSectionHeader(
            stringResource(R.string.news_feed_type_recommend),
            onAllClick = onAllClick
        )
    }
    if (list.isEmpty()) {
        item {
            NewsEmpty()
        }
    } else {
        items(list) { item -> NewsItem(Modifier, item, onClick = { onItemClick(item.newsId) }) }
    }

}

private fun LazyListScope.NewsRecent(
    list: List<NewsSummary>,
    onItemClick: (Long) -> Unit,
    onAllClick: () -> Unit,
) {
    item {
        NewsSectionHeader(
            stringResource(R.string.news_feed_type_recent),
            onAllClick = onAllClick
        )
    }
    if (list.isEmpty()) {
        item {
            NewsEmpty()
        }
    } else {
        items(list) { item -> NewsItem(Modifier, item, onClick = { onItemClick(item.newsId) }) }
    }
}

private fun LazyListScope.NewsAll(
    list: List<NewsSummary>,
    onItemClick: (Long) -> Unit,
) {
    if (list.isEmpty()) {
        item {
            NewsEmpty()
        }
    } else {
        items(list) { item ->
            NewsItem(Modifier, item, onClick = { onItemClick(item.newsId) })
        }
    }
}

enum class NewsFeedType(@StringRes val titleRes: Int) {
    HOT(R.string.news_feed_type_hot),
    RECOMMEND(R.string.news_feed_type_recommend),
    LATEST(R.string.news_feed_type_recent);

}