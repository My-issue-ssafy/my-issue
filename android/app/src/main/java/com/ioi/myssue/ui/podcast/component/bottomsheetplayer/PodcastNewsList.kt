package com.ioi.myssue.ui.podcast.component.bottomsheetplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.ui.podcast.KeyWordList

@Composable
fun PodcastNewsList(
    news: List<NewsSummary>,
    modifier: Modifier = Modifier
) {
    Text(
        "9월 4일 HOT 뉴스",
        fontWeight = FontWeight.Bold,
        color = BackgroundColors.Background50
    )

    KeyWordList(
        keywords = listOf("경제", "부동산", "주식", "코인", "금리", "대출", "경제", "부동산", "주식", "코인"),
        modifier = modifier.fillMaxWidth()
    )

    NewsSummaryList(news)
}

@Composable
private fun NewsSummaryList(news: List<NewsSummary>) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(rememberNestedScrollInteropConnection()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(news) { item ->
            NewsSummary(
                newsSummary = item,
                fontColor = BackgroundColors.Background50
            )
        }
    }
}

@Composable
fun NewsSummary(
    modifier: Modifier = Modifier,
    newsSummary: NewsSummary,
    fontColor: Color = Color.Black,
    onClick: (Long) -> Unit = { }
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable{onClick(newsSummary.newsId)}
    ) {
        Card(
            shape = RoundedCornerShape(4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.size(68.dp).aspectRatio(1f),
            colors = CardDefaults.cardColors(
                containerColor = BackgroundColors.Background50
            )
        ) {
            AsyncImage(
                model = newsSummary.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_empty_thumbnail),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = newsSummary.category,
                style = MaterialTheme.typography.labelSmall,
                color = fontColor.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = newsSummary.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = fontColor,
                maxLines = 2
            )
        }
    }
}
