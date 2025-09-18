package com.ioi.myssue.ui.news

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.AppColors.Primary600
import com.ioi.myssue.designsystem.theme.BackgroundColors.Background500
import com.ioi.myssue.domain.model.NewsBlock
import com.ioi.myssue.ui.common.clickableNoRipple
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetail(
    newsId: Long?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    viewModel: NewsDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(newsId) {
        viewModel.getNewsDetail(newsId)
    }
    val state = viewModel.uiState.collectAsState().value

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NewsDetailEffect.Toast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        containerColor = Color.White,
        dragHandle = { SheetDragHandle() },
    ) {
        NewsDetailSheet(
            title = state.title,
            author = state.author,
            newspaper = state.newspaper,
            displayTime = state.displayTime,
            blocks = state.blocks,
            isBookmarked = state.isBookmarked,
            onToggleBookmark = { viewModel.toggleBookmark() }
        )
    }
}

// 본문 스크롤할 때 시트 내려감 방지
@Composable
private fun rememberBlockSheetDragConnection(): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {
            // child(본문)가 스크롤 처리한 "뒤에" 남은 양은 전부 소비 → 시트로 안 올라가게
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = Offset(0f, available.y)

            // fling 시작 전에 child가 받을 수 있게 그대로 통과
            override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero

            // child가 처리하고 "남은" fling은 전부 소비 → 시트 플링으로 안 올라감
            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity = Velocity(0f, available.y)
        }
    }
}


// 기사 바텀 시트
@Composable
fun NewsDetailSheet(
    title: String,
    author: String,
    newspaper: String,
    displayTime: String,
    blocks: List<NewsBlock>,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        horizontalAlignment = Alignment.Start
    ) {
        NewsDetailHeader(
            title = title,
            author = author,
            newspaper = newspaper,
            displayTime = displayTime,
            isBookmarked = isBookmarked,
            onToggleBookmark = onToggleBookmark
        )
        Spacer(Modifier.height(12.dp))
        NewsDetailBody(
            blocks
        )
    }
}

// 시트 핸들
@Composable
fun SheetDragHandle() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 30.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(width = 90.dp, height = 5.dp)
                .background(
                    color = Primary600,
                    shape = CircleShape
                )
        )
    }
}

// 기사 헤더
@Composable
fun NewsDetailHeader(
    title: String,
    author: String,
    newspaper: String,
    displayTime: String,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
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
            // 신문사, 기자
            Row() {
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

// 기사 본문
@Composable
fun NewsDetailBody(
    blocks: List<NewsBlock>,
) {
    val scroll = rememberScrollState()
    val blockSheetDrag = rememberBlockSheetDragConnection()

    Column(
        modifier = Modifier
            .nestedScroll(blockSheetDrag)
            .verticalScroll(scroll)
    ) {
        blocks.forEach { block ->
            when (block) {
                is NewsBlock.Image -> NewsContentImage(url = block.url)
                is NewsBlock.Desc -> NewsContentDesc(text = block.text)
                is NewsBlock.Text -> NewsContentText(text = block.text)
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
            .padding(bottom = 8.dp)
            .aspectRatio(16 / 9f),
        contentScale = ContentScale.Crop,
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