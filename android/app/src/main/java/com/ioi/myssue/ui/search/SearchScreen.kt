package com.ioi.myssue.ui.search

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.common.collect.Multimaps.index
import com.ioi.myssue.LocalAnalytics
import com.ioi.myssue.ui.news.Loading
import com.ioi.myssue.ui.news.NewsDetail
import com.ioi.myssue.ui.news.NewsItem
import kotlinx.coroutines.launch

private const val TAG = "SearchScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState = viewModel.state.collectAsState().value
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = uiState.scrollIndex,
        initialFirstVisibleItemScrollOffset = uiState.scrollOffset
    )
    val scope = rememberCoroutineScope()
    val analytics = LocalAnalytics.current

    // 바닥에서 스크롤 시 다음 뉴스 로딩
    val loadMore by remember(uiState.newsItems.size, uiState.cursor) {
        Log.d("http loadmore test","")
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= uiState.newsItems.size - 5 && uiState.cursor != null
        }
    }
    LaunchedEffect(loadMore) {
        if (loadMore) viewModel.loadMore()
    }

    DisposableEffect(listState) {
        onDispose {
            viewModel.saveScrollState(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }
    }
    // ✅ 검색어나 카테고리 변경 시만 최상단으로 이동
    LaunchedEffect(uiState.selectedCat) {
        if(uiState.scrollIndex == 0 && uiState.scrollOffset == 0) {
            listState.scrollToItem(0, 0)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // 검색바
        SearchBar(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChange,
            onClear = viewModel::onClearQuery,
            onSearch = {
                scope.launch{ listState.scrollToItem(0, 0) }
                viewModel.onSearch()
            },

            modifier = Modifier.padding(horizontal = 12.dp)
        )
        // 카테고리 칩
        CategoryChipsRow(
            selected = uiState.selectedCat,
            onSelect = viewModel::onSelectCategory,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        when {
            // 로딩/빈화면
            uiState.isInitialLoading -> {
                Box(Modifier.fillMaxSize()) { Loading() }
            }

            uiState.newsItems.isEmpty() -> {
                Box(Modifier.fillMaxSize()) { SearchedEmpty() }
            }

            // 뉴스 목록 출력
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(
                        items = uiState.newsItems.distinctBy { it.newsId },
                        key = {  item -> "${item.newsId}" }
                    ) { item ->
                        NewsItem(
                            modifier = Modifier,
                            news = item,
                            onClick = {
                                viewModel.onItemClick(item.newsId)
                                analytics.logNewsClick(item.newsId, feedSource = "search")
                                Log.d(TAG, "logNewsClick: ${item.newsId} search")
                            }
                        )
                    }
                }
            }
        }

    }

    // 뉴스기사 바텀시트
    uiState.selectedId?.let { id ->
        NewsDetail(
            newsId = id,
            onDismiss = { viewModel.onItemClose() }
        )
    }
}