package com.ioi.myssue.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ioi.myssue.data.repository.fake.FakeNotificationRepositoryImpl
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.designsystem.ui.TopBarViewModel
import com.ioi.myssue.ui.main.MainActivity
import com.ioi.myssue.ui.news.Loading
import com.ioi.myssue.ui.news.NewsDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val topBarVM: TopBarViewModel = hiltViewModel(context as MainActivity)

    DisposableEffect(Unit) {
        onDispose {
            topBarVM.refreshUnread()
        }
    }

    LaunchedEffect(Unit) { viewModel.refresh() }

    // 하단 근처에서 다음 페이지 로드
    val shouldLoadMore by remember(uiState.items.size, uiState.hasNext) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= uiState.items.size - 4 && uiState.hasNext
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    if (uiState.isInitialLoading) {
        Box(Modifier.fillMaxSize()) { Loading() }
    } else if (uiState.items.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColors.Background100)
        ) {
            NotificationTopHeader(
                title = "최근 알림",
                isClearAllEnabled = false,
                onClickClearAll = { viewModel.deleteNotificationAll() }
            )
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                NotificationEmpty(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            stickyHeader {
                Box(
                    Modifier
                        .fillMaxWidth()
                ) {
                    NotificationTopHeader(
                        title = "최근 알림",
                        isClearAllEnabled = true,
                        onClickClearAll = { viewModel.deleteNotificationAll() }
                    )
                }
            }
            items(
                items = uiState.items,
                key = {
                    when (it) {
                        is NotificationListItem.Header -> "header_${it.type}"
                        is NotificationListItem.Row -> "row_${it.item.notificationId}"
                    }
                }
            ) { item ->
                when (item) {
                    is NotificationListItem.Header ->
                        NotificationDateHeader(item.type.label())

                    is NotificationListItem.Row ->
                        SwipeableNotificationItem(
                            n = item.item,
                            timeText = viewModel.formatTime(item.item.createdAt),
                            onClick = { viewModel.onItemClick(item.item) },
                            onDelete = { viewModel.deleteNotification(item.item) }
                        )
                }
            }
        }
    }

    // 바텀 시트
    uiState.selectedNewsId?.let { id ->
        NewsDetail(
            newsId = id,
            sheetState = sheetState,
            onDismiss = { viewModel.onItemClose() }
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun NotificationScreenPreview() {
    val time =
        remember { com.ioi.myssue.common.util.TimeConverter(java.time.ZoneId.systemDefault()) }
    val vm = remember { NotificationViewModel(FakeNotificationRepositoryImpl(), time) }

    LaunchedEffect(Unit) { vm.refresh() }
    NotificationScreen(viewModel = vm)
}
