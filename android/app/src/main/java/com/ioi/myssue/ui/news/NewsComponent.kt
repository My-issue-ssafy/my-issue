package com.ioi.myssue.ui.news

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.AppColors.Primary400
import com.ioi.myssue.designsystem.theme.AppColors.Primary600
import com.ioi.myssue.designsystem.theme.AppColors.Primary700
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.designsystem.theme.BackgroundColors.Background300
import com.ioi.myssue.designsystem.theme.BackgroundColors.Background400
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.ui.common.clickableNoRipple

@Composable
fun NewsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, style = typography.titleLarge
        )
        if (onAllClick != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.see_all),
                style = typography.labelLarge,
                color = Primary700,
                modifier = Modifier.clickableNoRipple { onAllClick() })
        }
    }
}

// HOT뉴스 페이저
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun HotNewsPager(
    items: List<NewsSummary>,
    onClick: (NewsSummary) -> Unit = {},
    peek: Dp = 28.dp,   // 다음 페이지 미리보기 길이
    pageSpacing: Dp = 18.dp
) {
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp
    val pageWidth = remember(screenWidth, peek, pageSpacing) {
        (screenWidth - (peek * 2) - pageSpacing)
    }
    val pagerState = rememberPagerState(pageCount = { items.size })

    Column {
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(pageWidth),
            pageSpacing = pageSpacing,
            contentPadding = PaddingValues(start = peek, end = peek),
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) { page ->
            HotNewsSlide(
                news = items[page],
                onClick = { onClick(items[page]) },
                modifier = Modifier.width(pageWidth)
            )
        }
        DotsIndicator(
            count = items.size,
            current = pagerState.currentPage,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

// HOT뉴스 페이저 한 슬라이드
@Composable
fun HotNewsSlide(
    news: NewsSummary, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
    ) {
        Box(Modifier.fillMaxSize()) {
            if (news.thumbnail == null) {
                Image(
                    painter = painterResource(R.drawable.ic_empty_thumbnail),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = news.thumbnail,
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
                            0.3f to Color.Transparent,
                            0.7f to Color(0x66000000),
                            1f to Color(0x99000000)
                        )
                    )
            )

            // 카테고리
            AssistChip(
                onClick = onClick,
                label = { Text(news.category, color = BackgroundColors.Background50) },
                shape = CircleShape,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Primary600
                ),
                border = null,
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp)
                    .align(Alignment.TopStart)
                    .height(20.dp)
            )

            // 기사 제목
            Text(
                text = news.title,
                style = typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = BackgroundColors.Background50,
                maxLines = 1,
                minLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
fun DotsIndicator(
    modifier: Modifier = Modifier,
    count: Int,
    current: Int,
    dotSize: Dp = 8.dp,
    activeDotWidth: Dp = 22.dp,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(count) { index ->
            val active = current == index
            val width by animateDpAsState(
                targetValue = if (active) activeDotWidth else dotSize, animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 3000f
                ), label = "dotWidth"
            )
            val color: Color by animateColorAsState(
                targetValue = if (active) {
                    Primary700
                } else {
                    Background300
                }, animationSpec = tween(
                    durationMillis = 300,
                )
            )

            Box(
                Modifier
                    .padding(horizontal = 5.dp)
                    .height(dotSize)
                    .width(width)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(color)
            )
        }
    }
}

// 뉴스 목록 한 줄
@Composable
fun NewsItem(
    modifier: Modifier = Modifier,
    news: NewsSummary,
    isMarked: Boolean = false,
    onClick: (NewsSummary) -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .padding(16.dp, 8.dp)
            .clickableNoRipple { onClick(news) }) {
        NewsThumbnail(img = news.thumbnail)

        Spacer(Modifier.width(12.dp))

        Column(
            Modifier
                .weight(1f)
                .padding(top = 5.dp, bottom = 5.dp, start = 5.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if(isMarked){
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NewsCategory(category = news.category)
                    Image(
                        painter = painterResource(R.drawable.ic_my_bookmark),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            else {
                NewsCategory(category = news.category)
            }

            NewsTitle(title = news.title)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NewsCreatedAt(relativeTime = news.relativeTime)
                NewsViews(views = news.views)
            }
        }
    }
}

// 뉴스 썸네일 이미지
@Composable
fun NewsThumbnail(img: String?) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundColors.Background50
        ),
        modifier = Modifier.size(100.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        AsyncImage(
            model = img,
            contentDescription = null,
            placeholder = painterResource(R.drawable.ic_empty_thumbnail),
            error = painterResource(R.drawable.ic_empty_thumbnail),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}


// 뉴스 카테고리
@Composable
fun NewsCategory(category: String) {
    Text(
        text = category,
        style = typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = Background400
    )
}

// 뉴스 제목
@Composable
fun NewsTitle(title: String) {
    Text(
        text = title,
        style = typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        minLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

// 뉴스 시간
@Composable
fun NewsCreatedAt(relativeTime: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_time),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = relativeTime,
            style = typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Background400,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

// 뉴스 조회수
@Composable
fun NewsViews(views: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_eye),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Background400
        )
        Text(
            text = "$views",
            style = typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Background400,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}


// 뉴스 없을 때 화면
@Composable
fun NewsEmpty() {
    Text(
        text = stringResource(R.string.no_news),
        style = typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Background400,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    )
}

// 뉴스 로딩 화면
@Composable
fun Loading() {
    Box(Modifier
        .fillMaxSize()
    ) {
        CircularProgressIndicator(
            color = Primary400,
            modifier = Modifier
                .align(Alignment.Center)
        )
    }
}