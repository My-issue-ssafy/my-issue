package com.ioi.myssue.ui.news

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ioi.myssue.domain.model.News


@Composable
fun NewsScreen(
    viewModel: NewsViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsState()

    val hot = state.value.hot
    val rec = state.value.recommend
    val recent = state.value.recent

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp)
    ) {
        NewsHOT(state.value.hot)
        NewsRecommend(state.value.recommend)
        NewsRecent(state.value.recent)
    }
}


fun LazyListScope.NewsHOT(list: List<News>) {
    if (list.isEmpty()) return
    item { NewsSectionHeader("HOT 뉴스") }
    item {
        HotNewsPager(items = list)
    }
}
fun LazyListScope.NewsRecommend(list: List<News>) {
    if (list.isEmpty()) return
    item { NewsSectionHeader("맞춤 뉴스") }
    items(list) { item -> NewsItem(item) }
}

private fun LazyListScope.NewsRecent(list: List<News>) {
    if (list.isEmpty()) return
    item { NewsSectionHeader("최신 뉴스") }
    items(list) { item -> NewsItem(item) }
}