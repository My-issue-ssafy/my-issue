package com.ioi.myssue.ui.news

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ioi.myssue.LocalAnalytics
import com.ioi.myssue.R
import com.ioi.myssue.analytics.AnalyticsLogger
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.domain.model.NewsSummary
import kotlinx.coroutines.launch
import com.ioi.myssue.ui.main.LocalDeepLinkNewsId
import com.ioi.myssue.ui.main.LocalConsumeDeepLinkNewsId

private const val TAG = "NewsScreen"

val NewsFeedType.title: String
    @Composable get() = stringResource(titleRes)

// 뉴스 메인 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    viewModel: NewsMainViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    val analytics = LocalAnalytics.current

    // 딥링크 id 구독
    val deepLinkId = LocalDeepLinkNewsId.current
    val consumeDeepLink = LocalConsumeDeepLinkNewsId.current
    // 딥링크 클릭 시 기사 상세 오픈
    LaunchedEffect(deepLinkId) {
        val id = deepLinkId
        if (id != null) {
            viewModel.onItemClick(id)
            consumeDeepLink()
        }
    }

    LaunchedEffect(Unit) { viewModel.getNews() }

    when {
        uiState.isInitialLoading -> { /* ... */
        }

        else -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                NewsHOT(
                    uiState.main.hot,
                    viewModel::onItemClick,
                    { viewModel.onClickSeeAll(NewsFeedType.HOT) },
                    analytics
                )
                if (uiState.main.recommend.isNotEmpty()) {
                    NewsRecommend(
                        uiState.main.recommend,
                        viewModel::onItemClick,
                        { viewModel.onClickSeeAll(NewsFeedType.RECOMMEND) },
                        analytics
                    )
                }
                NewsLatest(
                    uiState.main.latest,
                    viewModel::onItemClick,
                    { viewModel.onClickSeeAll(NewsFeedType.LATEST) },
                    analytics
                )
            }
        }
    }

    uiState.selectedId?.let { id ->
        NewsDetail(
            newsId = id,
            onDismiss = {
                viewModel.onItemClose()
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
    val listState = rememberLazyListState()
    val analytics = LocalAnalytics.current

    LaunchedEffect(type) {
        when (type) {
            NewsFeedType.HOT -> viewModel.getAllHotNews()
            NewsFeedType.RECOMMEND -> viewModel.getAllRecommendNews()
            NewsFeedType.LATEST -> viewModel.getAllRecentNews()
        }
    }

    // 바닥에서 스크롤 시 다음 뉴스 로딩
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

    when {
        uiState.isInitialLoading -> {
            Box(
                Modifier
                    .fillMaxSize()
            ) { Loading() }
        }

        else -> {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(4.dp)
            ) {
                item { NewsSectionHeader(type.title, onAllClick = null) }
                NewsAll(
                    list = uiState.items,
                    onItemClick = viewModel::onItemClick,
                    analytics = analytics,
                    feedSource = when (type) {
                        NewsFeedType.HOT -> "hot"
                        NewsFeedType.RECOMMEND -> "recommend"
                        NewsFeedType.LATEST -> "latest"
                    }
                )
            }
        }
    }

    // 뉴스기사 바텀시트
    uiState.selectedId?.let { id ->
        NewsDetail(
            newsId = id,
            onDismiss = {
                viewModel.onItemClose()
            }
        )
    }
}

fun LazyListScope.NewsHOT(
    list: List<NewsSummary>,
    onItemClick: (Long) -> Unit,
    onAllClick: () -> Unit,
    analytics: AnalyticsLogger
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
                onClick = { news ->
                    onItemClick(news.newsId)
                    analytics.logNewsClick(news.newsId, feedSource = "hot")
                    Log.d(TAG, "logNewsClick: ${news.newsId} hot")
                }
            )
        }
    }
}

fun LazyListScope.NewsRecommend(
    list: List<NewsSummary>,
    onItemClick: (Long) -> Unit,
    onAllClick: () -> Unit,
    analytics: AnalyticsLogger
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
        items(list) { item ->
            NewsItem(
                modifier = Modifier,
                news = item,
                onClick = {
                    onItemClick(item.newsId)
                    analytics.logNewsClick(item.newsId, feedSource = "recommend")
                    Log.d(TAG, "logNewsClick: ${item.newsId} recommend")
                }
            )
        }
    }
}

private fun LazyListScope.NewsLatest(
    list: List<NewsSummary>,
    onItemClick: (Long) -> Unit,
    onAllClick: () -> Unit,
    analytics: AnalyticsLogger
) {
    item {
        NewsSectionHeader(
            stringResource(R.string.news_feed_type_latest),
            onAllClick = onAllClick
        )
    }
    if (list.isEmpty()) {
        item {
            NewsEmpty()
        }
    } else {
        items(list) { item ->
            NewsItem(
                modifier = Modifier,
                news = item,
                onClick = {
                    onItemClick(item.newsId)
                    analytics.logNewsClick(item.newsId, feedSource = "latest")
                    Log.d(TAG, "logNewsClick: ${item.newsId} latest")
                }
            )
        }
    }
}

private fun LazyListScope.NewsAll(
    list: List<NewsSummary>,
    onItemClick: (Long) -> Unit,
    feedSource: String,
    analytics: AnalyticsLogger
) {
    if (list.isEmpty()) {
        item {
            NewsEmpty()
        }
    } else {
        items(list) { item ->
            NewsItem(
                modifier = Modifier,
                news = item,
                onClick = {
                    analytics.logNewsClick(item.newsId, feedSource = feedSource)
                    onItemClick(item.newsId)
                    Log.d(TAG, "logNewsClick: ${item.newsId} $feedSource")
                }
            )
        }
    }
}

enum class NewsFeedType(@StringRes val titleRes: Int) {
    HOT(R.string.news_feed_type_hot),
    RECOMMEND(R.string.news_feed_type_recommend),
    LATEST(R.string.news_feed_type_latest);

}