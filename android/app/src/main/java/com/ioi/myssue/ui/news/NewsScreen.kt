package com.ioi.myssue.ui.news

import android.util.Log
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.domain.model.News

val NewsFeedType.title: String
    @Composable get() = stringResource(titleRes)

// 뉴스 메인 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    viewModel: NewsMainViewModel = hiltViewModel(),
) {
    val items = viewModel.state.collectAsState()
    var selected by rememberSaveable { mutableStateOf<News?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp)
    ) {
        NewsHOT(
            list = items.value.hot,
            onItemClick = { selected = it },
            onAllClick = { viewModel.onClickSeeAll(NewsFeedType.HOT) }
        )
        NewsRecommend(
            list = items.value.recommend,
            onItemClick = { selected = it },
            onAllClick = { viewModel.onClickSeeAll(NewsFeedType.RECOMMEND) }
        )
        NewsRecent(
            list = items.value.recent,
            onItemClick = { selected = it },
            onAllClick = { viewModel.onClickSeeAll(NewsFeedType.RECENT) }
        )
    }

    // 바텀시트
    if (selected != null) {
        NewsDetail(
            newsId = selected!!.id,
            sheetState = sheetState,
            onDismiss = { selected = null }
        )
    }
}

// 섹션별 전체보기 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsAllScreen(
    type: NewsFeedType,
    viewModel: NewsAllViewModel = hiltViewModel()
) {
    LaunchedEffect(type) {
        when (type) {
            NewsFeedType.HOT -> viewModel.getAllHotNews()
            NewsFeedType.RECOMMEND -> viewModel.getAllRecommendNews()
            NewsFeedType.RECENT -> viewModel.getAllRecentNews()
        }
    }

    val page by viewModel.state.collectAsState()
    val items = page.items

    var selected by rememberSaveable { mutableStateOf<News?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 바닥에서 스크롤 시 다음 뉴스 로딩
    val listState = rememberLazyListState()
    val loadMore by remember(items.size, page.nextCursor) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= items.size - 5 && page.nextCursor != null
        }
    }
    LaunchedEffect(loadMore) {
        if (loadMore) {
            when (type) {
                NewsFeedType.HOT -> viewModel.getAllHotNews(page.nextCursor)
                NewsFeedType.RECOMMEND -> viewModel.getAllRecommendNews(page.nextCursor)
                NewsFeedType.RECENT -> viewModel.getAllRecentNews(page.nextCursor)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(BackgroundColors.Background50),
        contentPadding = PaddingValues(4.dp)
    ) {
        item { NewsSectionHeader(type.title, onAllClick = null) }
        NewsAll(
            list = items,
            onItemClick = { selected = it }
        )
    }

    // 바텀시트
    if (selected != null) {
        NewsDetail(
            newsId = selected!!.id,
            sheetState = sheetState,
            onDismiss = { selected = null }
        )
    }
}

fun LazyListScope.NewsHOT(
    list: List<News>,
    onItemClick: (News) -> Unit,
    onAllClick: () -> Unit,
) {
    if (list.isEmpty()) return
    item { NewsSectionHeader(stringResource(R.string.news_feed_type_hot), onAllClick = onAllClick) }
    item {
        HotNewsPager(items = list, onClick = onItemClick)
    }
}

fun LazyListScope.NewsRecommend(
    list: List<News>,
    onItemClick: (News) -> Unit,
    onAllClick: () -> Unit,
) {
    if (list.isEmpty()) return
    item {
        NewsSectionHeader(
            stringResource(R.string.news_feed_type_recommend),
            onAllClick = onAllClick
        )
    }
    items(list) { item -> NewsItem(Modifier, item, onClick = { onItemClick(item) }) }
}

private fun LazyListScope.NewsRecent(
    list: List<News>,
    onItemClick: (News) -> Unit,
    onAllClick: () -> Unit,
) {
    if (list.isEmpty()) return
    item {
        NewsSectionHeader(
            stringResource(R.string.news_feed_type_recent),
            onAllClick = onAllClick
        )
    }
    items(list) { item -> NewsItem(Modifier, item, onClick = { onItemClick(item) }) }
}

private fun LazyListScope.NewsAll(
    list: List<News>,
    onItemClick: (News) -> Unit,
) {
    if (list.isEmpty()) return
    items(list) { item -> NewsItem(Modifier, item, onClick = { onItemClick(item) }) }
}


enum class NewsFeedType(@StringRes val titleRes: Int) {
    HOT(R.string.news_feed_type_hot),
    RECOMMEND(R.string.news_feed_type_recommend),
    RECENT(R.string.news_feed_type_recent);

}