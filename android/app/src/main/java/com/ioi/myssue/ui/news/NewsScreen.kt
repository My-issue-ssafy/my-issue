package com.ioi.myssue.ui.news

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ioi.myssue.domain.model.News

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    viewModel: NewsViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsState()
    var selected by rememberSaveable { mutableStateOf<News?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp)
    ) {
        NewsHOT(state.value.hot) { selected = it }
        NewsRecommend(state.value.recommend) { selected = it }
        NewsRecent(state.value.recent) { selected = it }
    }

    // 바텀시트
    if (selected != null) {
        NewsDetail(
            news = selected!!,
            sheetState = sheetState,
            onDismiss = { selected = null }
        )
    }
}


fun LazyListScope.NewsHOT(list: List<News>, onItemClick: (News) -> Unit) {
    if (list.isEmpty()) return
    item { NewsSectionHeader("HOT 뉴스") }
    item {
        HotNewsPager(items = list, onClick = onItemClick)
    }
}

fun LazyListScope.NewsRecommend(list: List<News>, onItemClick: (News) -> Unit) {
    if (list.isEmpty()) return
    item { NewsSectionHeader("맞춤 뉴스") }
    items(list) { item -> NewsItem(Modifier, item, onClick = { onItemClick(item) }) }
}

private fun LazyListScope.NewsRecent(list: List<News>, onItemClick: (News) -> Unit) {
    if (list.isEmpty()) return
    item { NewsSectionHeader("최신 뉴스") }
    items(list) { item -> NewsItem(Modifier, item, onClick = { onItemClick(item) }) }
}