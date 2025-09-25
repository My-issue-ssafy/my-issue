package com.ioi.myssue.ui.mypage

import android.R.attr.strokeWidth
import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioi.myssue.LocalAnalytics
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.domain.model.CartoonNews
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.ui.cartoon.CartoonCard
import com.ioi.myssue.ui.news.DotsIndicator
import com.ioi.myssue.ui.news.NewsDetail
import com.ioi.myssue.ui.news.NewsSectionHeader
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.ioi.myssue.ui.podcast.component.bottomsheetplayer.NewsSummary

private const val TAG = "MyPageScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    viewModel: MyPageViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val analytics = LocalAnalytics.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColors.Background100)
    ) {
        NewsSectionHeader(
            title = "내가 스크랩한 뉴스",
            modifier = Modifier.padding(top = 8.dp),
            onAllClick = if (state.newsSummaries.isNotEmpty()) ({ viewModel.navigateToAllScraps() }) else null
        )
        if (state.isLoadingScrapNews) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(vertical = 128.dp)
                    .align(Alignment.CenterHorizontally),
                color = AppColors.Primary600,
                strokeWidth = 2.dp
            )
        } else {
            ScrappedNewsPager(
                items = state.newsSummaries,
                navigateToCartoonNews = viewModel::navigateToNews,
                openNewsDetail = { id ->
                    viewModel.openNewsDetail(id)
                    analytics.logNewsClick(id, feedSource = "scrap")
                    Log.d(TAG, "logNewsClick: $id scrap")
                }
            )
        }
        NewsSectionHeader(
            title = "내가 좋아한 네컷뉴스",
            modifier = Modifier.padding(top = 12.dp),
            onAllClick = if (state.myToons.isNotEmpty()) ({ viewModel.navigateToAllToons() }) else null
        )
        if (state.isLoadingLikeToons) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(vertical = 80.dp).align(Alignment.CenterHorizontally),
                    color = AppColors.Primary600,
                    strokeWidth = 2.dp
                )
        } else {
            ScrappedCartoonNewsPager(
                items = state.myToons,
                modifier = Modifier.weight(1f),
                navigateToNews = viewModel::navigateToCartoonNews
            )
        }
    }

    state.selectedNewsId?.let{
        NewsDetail(
            newsId = it,
            onDismiss = { viewModel.closeNewsDetail() }
        )
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun ColumnScope.ScrappedCartoonNewsPager(
    modifier: Modifier = Modifier,
    items: List<CartoonNews>,
    pageSpacing: Dp = 12.dp,
    navigateToNews: () -> Unit = { }
) {
    if (items.isEmpty()) {
        NoContentWithShotCut(
            text = "좋아요한 네컷뉴스가 없습니다.",
            buttonText = "네컷뉴스 보러가기",
        ) { navigateToNews() }
        return
    }

    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp
    val density = LocalDensity.current

    var cardWidth by remember { mutableStateOf(0.dp) }

    // 카드 폭을 알아야 peek 계산 가능
    val peek = remember(screenWidth, cardWidth) {
        if (cardWidth > 0.dp) {
            ((screenWidth - cardWidth) / 2).coerceAtLeast(0.dp)
        } else {
            0.dp
        }
    }

    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            pageSize = if (cardWidth > 0.dp) PageSize.Fixed(cardWidth) else PageSize.Fill,
            pageSpacing = pageSpacing,
            contentPadding = PaddingValues(start = peek, end = peek),
            modifier = Modifier.weight(1f)
        ) { page ->

            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val distance = abs(pageOffset)

            val blur by animateDpAsState(
                targetValue = (distance * 2).dp, // 20dp 정도 블러
            )

            CartoonCard(
                cartoon = items[page],
                isExiting = false,
                exitDir = 0,
                isSmallMode = true,
                modifier = Modifier
                    .onSizeChanged {
                        cardWidth = with(density) { it.width.toDp() }
                    }
                    .blur(blur)
                    .align(Alignment.CenterHorizontally)
                    .padding(12.dp),
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                }
            )
        }

        DotsIndicator(
            count = items.size,
            current = pagerState.currentPage,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ColumnScope.NoContentWithShotCut(
    text: String,
    buttonText: String,
    openScreen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(text)
        Button(
            onClick = openScreen,
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = AppColors.Primary500,
            )
        ) {
            Text(buttonText)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.ScrappedNewsPager(
    items: List<NewsSummary>,
    modifier: Modifier = Modifier,
    pageSize: Int = 3,
    navigateToCartoonNews: () -> Unit = { },
    openNewsDetail: (Long) -> Unit = { },
    itemContent: @Composable (NewsSummary) -> Unit = {
        NewsSummary(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            newsSummary = it,
            onClick = openNewsDetail
        )
    }
) {
    if (items.isEmpty()) {
        NoContentWithShotCut(
            text = "스크랩한 뉴스가 없습니다.",
            buttonText = "뉴스 보러가기",
        ) { navigateToCartoonNews() }
        return
    }

    val pages = remember(items, pageSize) { items.chunked(pageSize) }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    HorizontalPager(
        state = pagerState,
        modifier = modifier
    ) { page ->
        val pageItems = pages.getOrNull(page).orEmpty()

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            pageItems.forEach { item ->
                itemContent(item)
            }

            repeat(pageSize - pageItems.size) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp) // itemContent 높이와 동일하게
                )
            }
        }

    }
    Spacer(Modifier.height(8.dp))
    DotsIndicator(
        count = (items.size - 1) / pageSize + 1,
        current = pagerState.currentPage,
        dotSize = 8.dp
    )
}