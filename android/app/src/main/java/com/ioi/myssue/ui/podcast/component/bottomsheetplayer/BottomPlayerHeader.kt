package com.ioi.myssue.ui.podcast.component.bottomsheetplayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.ui.common.clickableNoRipple
import com.ioi.myssue.ui.podcast.PodcastContentType

@Composable
fun BottomPlayerHeader(
    thumbnailUrl: String,
    title: String,
    dateText: String,
    contentType: PodcastContentType,
    toggleContentType: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = RoundedCornerShape(6.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundColors.Background50),
            modifier = Modifier.size(80.dp)
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = BackgroundColors.Background50.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BackgroundColors.Background50
            )
        }

        Image(
            painterResource(
                if (contentType == PodcastContentType.NEWS) R.drawable.ic_podcast_list_pressed
                else R.drawable.ic_podcast_list
            ),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clickableNoRipple(toggleContentType)
        )
    }
}
