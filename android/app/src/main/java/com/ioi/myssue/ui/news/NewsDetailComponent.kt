package com.ioi.myssue.ui.news

import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ioi.myssue.LocalAnalytics
import com.ioi.myssue.R
import com.ioi.myssue.analytics.AnalyticsLogger
import com.ioi.myssue.designsystem.theme.BackgroundColors.Background500
import com.ioi.myssue.designsystem.ui.MyssueBottomSheet
import com.ioi.myssue.domain.model.NewsBlock
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.ui.chat.ChatBotContent
import com.ioi.myssue.ui.chat.ChatBotViewModel
import com.ioi.myssue.ui.common.clickableNoRipple
import com.ioi.myssue.ui.main.MainActivity
import com.ioi.myssue.ui.podcast.component.bottomsheetplayer.NewsSummaryWithPublisher
import kotlinx.coroutines.flow.collectLatest

private const val TAG = "NewsDetailComponent"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetail(
    newsId: Long,
    onDismiss: () -> Unit,
    viewModel: NewsDetailViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsState().value
    val listState = rememberLazyListState()
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    val analytics = LocalAnalytics.current

    LaunchedEffect(newsId) {
        viewModel.getNewsDetail(newsId)
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NewsDetailEffect.Toast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    TrackNewsViewEndLazy(
        newsId = state.newsId,
        listState = listState,
        analytics = analytics,
        itemHeights = itemHeights,
    )

    CustomModalBottomSheetDialog(
        onDismiss = onDismiss,
        headerContent = {
            NewsDetailHeader(
                title = state.title,
                author = state.author,
                newspaper = state.newspaper,
                displayTime = state.displayTime,
                isBookmarked = state.isBookmarked,
                onToggleBookmark = viewModel::toggleBookmark,
            )
        }
    ) {

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 72.dp)
            ) {
                itemsIndexed(state.blocks) { index, block ->
                    // ⬇️ 각 아이템 높이 기록
                    Column(Modifier.onSizeChanged { size -> itemHeights[index] = size.height }) {
                        when (block) {
                            is NewsBlock.Image -> NewsContentImage(url = block.url)
                            is NewsBlock.Desc -> NewsContentDesc(text = block.text)
                            is NewsBlock.Text -> NewsContentText(text = block.text)
                        }
                    }
                }
            }
            ChatbotButton { viewModel.openChat() }
        }
    }

    if (state.c) {
        val chatViewModel: ChatBotViewModel = hiltViewModel(context as MainActivity)
        val chatState by chatViewModel.state.collectAsState()
        MyssueBottomSheet(
            onDismissRequest = viewModel::closeChat,
            headerContent = {
                val newsSummary = remember { NewsSummary(
                    newsId = state.newsId,
                    title = state.title,
                    author = state.author,
                    newspaper = state.newspaper,
                    thumbnail = state.thumbnail,
                ) }

                LaunchedEffect(newsId) {
                    if(chatState.newsSummary.newsId != newsId) {
                        chatViewModel.initData(newsSummary)
                    }
                }

                NewsSummaryWithPublisher(
                    newsSummary = newsSummary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                )
            }
        ) {
            ChatBotContent(viewModel = chatViewModel)
        }
    }
}

@Composable
fun BoxScope.ChatbotButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val iconRes = if (isPressed) {
        R.drawable.ic_chatbot_pressed
    } else {
        R.drawable.ic_chatbot
    }

    Image(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = modifier
            .padding(12.dp)
            .size(60.dp)
            .shadow(
                elevation = 2.dp,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            }
            .align(Alignment.BottomEnd)
    )
}


// 기사 헤더
@Composable
fun NewsDetailHeader(
    title: String,
    author: String,
    newspaper: String,
    displayTime: String,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .padding(horizontal = 18.dp),
    ) {
        Text(
            text = title,
            style = typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Row {
                Text(
                    text = newspaper,
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Background500
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = author,
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Background500
                )
            }

            // 발행 시간, 스크랩
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = displayTime,
                    style = typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Background500
                )
                Spacer(Modifier.width(12.dp))
                Image(
                    painter = painterResource(
                        if (isBookmarked) R.drawable.ic_bookmark_pressed
                        else R.drawable.ic_bookmark
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clickableNoRipple { onToggleBookmark() }
                )
            }
        }
    }
}

// 기사 이미지
@Composable
private fun NewsContentImage(url: String) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentScale = ContentScale.FillWidth,
        placeholder = painterResource(R.drawable.ic_empty_thumbnail),
    )
}

// 기사 이미지 설명 텍스트
@Composable
private fun NewsContentDesc(text: String) {
    Text(
        text = text,
        style = typography.labelMedium,
        color = Background500,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(top = 4.dp, bottom = 12.dp)
    )
}

// 기사 본문 텍스트
@Composable
private fun NewsContentText(text: String) {
    Text(
        text = text,
        style = typography.bodyMedium.copy(
            lineBreak = LineBreak.Paragraph
        ),
        softWrap = true,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun TrackNewsViewEndLazy(
    newsId: Long?,
    listState: LazyListState,
    analytics: AnalyticsLogger,
    itemHeights: Map<Int, Int>,
) {
    if (newsId == null) return
    val sessionStart = remember(newsId) { SystemClock.elapsedRealtime() }
    var maxPct by remember(newsId) { mutableIntStateOf(0) }

    LaunchedEffect(newsId, listState, itemHeights) {
        snapshotFlow {
            val info = listState.layoutInfo
            val totalCount = info.totalItemsCount

            // 뷰포트 높이
            val viewportPx = (info.viewportEndOffset - info.viewportStartOffset).coerceAtLeast(1)

            // 평균/총 높이 추정
            val knownHeights = itemHeights.values
            val knownTotalPx = knownHeights.sum()
            val avgPx = if (knownHeights.isNotEmpty()) knownHeights.average().toInt()
                .coerceAtLeast(1) else 1
            val unknownCount = (totalCount - itemHeights.size).coerceAtLeast(0)
            val estimatedTotalPx = knownTotalPx + unknownCount * avgPx

            // 스크롤된 픽셀: 이전 아이템들의 높이 합 + 현재 오프셋
            val firstIndex = listState.firstVisibleItemIndex
            val offsetPx = listState.firstVisibleItemScrollOffset
            var beforePx = 0
            if (firstIndex > 0) {
                // 0 until firstIndex 의 높이 합(미측정 아이템은 avg로 대체)
                beforePx = (0 until firstIndex).sumOf { idx -> itemHeights[idx] ?: avgPx }
            }
            val scrolledPx = (beforePx + offsetPx).coerceAtLeast(0)

            val maxScrollablePx = (estimatedTotalPx - viewportPx).coerceAtLeast(1)

            // 끝까지 도달 체크(시각적으로 마지막까지 밀면 100%)
            val atEnd = !listState.canScrollForward && totalCount > 0
            val pct = if (atEnd) 100
            else ((scrolledPx.toFloat() / maxScrollablePx.toFloat()) * 100f)
                .toInt()
                .coerceIn(0, 99)

            pct
        }.collect { pct ->
            if (pct > maxPct) maxPct = pct
        }
    }

    DisposableEffect(newsId) {
        onDispose {
            val dwell = SystemClock.elapsedRealtime() - sessionStart
            if (newsId < 0 || dwell < 1000) return@onDispose
            analytics.logNewsViewEnd(newsId, dwellMs = dwell, scrollPct = maxPct)
            Log.d("logNewsViewEnd", "newsId=$newsId dwell=$dwell scroll=$maxPct")
        }
    }
}
