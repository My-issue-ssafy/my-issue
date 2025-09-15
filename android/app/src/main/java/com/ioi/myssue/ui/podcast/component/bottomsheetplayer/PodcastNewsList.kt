package com.ioi.myssue.ui.podcast.component.bottomsheetplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.ui.podcast.KeyWordList

@Composable
fun PodcastNewsList(
    news: List<String>,
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

    PodcastNewsList(news)
}

@Composable
private fun PodcastNewsList(news: List<String>) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .nestedScroll(rememberNestedScrollInteropConnection()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(news) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    shape = RoundedCornerShape(4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    AsyncImage(
                        model = "https://imgnews.pstatic.net/image/277/2025/09/12/0005651015_001_20250912132818390.jpg?type=w860",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "IT",
                        style = MaterialTheme.typography.labelSmall,
                        color = BackgroundColors.Background50
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "5년 만에 ‘보급형 태블릿’ 띄우는 삼성... 신흥국 점령 나선다",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = BackgroundColors.Background50,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
