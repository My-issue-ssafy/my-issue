package com.ioi.myssue.ui.news

import android.R.attr.onClick
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.FloatingActionButtonDefaults.elevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.AppColors.Primary600
import com.ioi.myssue.designsystem.theme.AppColors.Primary700
import com.ioi.myssue.designsystem.theme.BackgroundColors.Background400
import com.ioi.myssue.domain.model.News
import java.sql.Time

@Composable
fun NewsSectionHeader(
    title: String,
//    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = typography.titleLarge
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "전체보기",
            style = typography.labelLarge,
            color = Primary700,
            modifier = Modifier.clickable { }
        )
    }
}

// HOT뉴스 페이저
@Composable
fun HotNewsPager(
    items: List<News>,
    onClick: (News) -> Unit = {},
    peek: Dp = 28.dp,   // 다음 페이지 미리보기 길이
    pageSpacing: Dp = 18.dp
) {
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp
    val pageWidth = remember(screenWidth, peek, pageSpacing) {
        (screenWidth - (peek * 2) - pageSpacing)
    }
    val pagerState = rememberPagerState(pageCount = { items.size })

    // 페이지에 따라 padding 조정
    val startPad = remember {
        derivedStateOf { if (pagerState.currentPage == 0) 28.dp else peek }
    }
    val endPad = remember {
        derivedStateOf { if (pagerState.currentPage == items.lastIndex) 28.dp else peek }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(pageWidth),
            pageSpacing = pageSpacing,
            contentPadding = PaddingValues(start = startPad.value, end = endPad.value),
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) { page ->
            HotNewsSlide(
                news = items[page],
                onClick = { onClick(items[page]) },
                modifier = Modifier.fillMaxSize()
                    .width(pageWidth)
                    .fillMaxHeight()
            )
        }
        DotsIndicator(
            count = items.size,
            current = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// HOT뉴스 페이저 한 슬라이드
@Composable
fun HotNewsSlide(
    news: News,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
    ) {
        Box(Modifier.fillMaxSize()) {
            if (news.img.isBlank()) {
                Image(
                    painter = painterResource(R.drawable.ic_empty_thumbnail),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = news.img,
                    contentDescription = news.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 하단 그라데이션
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.6f to Color(0x66000000),
                            1f to Color(0x99000000)
                        )
                    )
            )

            // 카테고리
            AssistChip(
                onClick = onClick,
                label = { Text(news.category, color = Color.White) },
                shape = CircleShape,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Primary600
                ),
                border = null,
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp)
                    .align(Alignment.TopStart)
            )

            // 기사 제목
            Text(
                text = news.title,
                style = typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                minLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun DotsIndicator(
    count: Int,
    current: Int,
    modifier: Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(count) { index ->
            val active = current == index
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

// 뉴스 목록 한 줄
@Composable
fun NewsItem(
    news: News,
    onClick: (News) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .padding(16.dp, 8.dp)
            .clickable { onClick(news) }
    ) {
        NewsThumbnail(img = news.img)

        Spacer(Modifier.width(12.dp))

        Column(
            Modifier
                .weight(1f)
                .padding(top = 5.dp, bottom = 5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NewsCategory(category = news.category)
            NewsTitle(title = news.title)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NewsCreatedAt(createdAt = news.createdAt)
                NewsViews(views = news.views)
            }
        }
    }
}

@Composable
// 뉴스 썸네일 이미지
fun NewsThumbnail(img: String?) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        modifier = Modifier
            .size(80.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_empty_thumbnail),
            contentDescription = null,
        )
    }
}


@Composable
// 뉴스 카테고리
fun NewsCategory(category: String) {
    Text(
        text = category,
        style = typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Background400
    )
}

@Composable
// 뉴스 제목
fun NewsTitle(title: String) {
    Text(
        text = title,
        style = typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        minLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
// 뉴스 시간
fun NewsCreatedAt(createdAt: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_time),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = createdAt,
            style = typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Background400,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
// 뉴스 조회수
fun NewsViews(views: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_eye),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Background400
        )
        Text(
            text = "$views",
            style = typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Background400,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
