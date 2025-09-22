package com.ioi.myssue.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.ui.podcast.component.bottomsheetplayer.NewsSummaryWithPublisher

@Composable
fun ChatBotContent(
    newsSummary: NewsSummary,
    viewModel: ChatBotViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(AppColors.Primary300, AppColors.Primary500))
            )
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)
    ) {
        NewsSummaryWithPublisher(
            newsSummary = newsSummary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            item {
                Image(
                    painter = painterResource(R.drawable.ic_chatbot),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(elevation = 4.dp, shape = CircleShape)
                )
                Spacer(Modifier.height(4.dp))
            }
            itemsIndexed(state.messages) { index, item ->
                val prevIsUser = if (index <= 0) false else state.messages[index - 1].isUser
                ChatMessage(item.text, item.isUser, prevIsUser == item.isUser)
            }
        }
        Spacer(Modifier.height(12.dp))
        ChatInputBar(
            value = state.inputMessage,
            onValueChange = viewModel::updateInputMessage,
            onSendClick = viewModel::sendMessage
        )
    }
}

@Composable
fun ChatInputBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = BackgroundColors.Background50.copy(alpha = 0.25f), // 배경 색상
                shape = RoundedCornerShape(24.dp)
            )
            .border(1.dp, AppColors.Primary100, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 입력 필드
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("메시지를 입력하세요", style = MaterialTheme.typography.bodyMedium) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = BackgroundColors.Background50)
        )

        // 전송 버튼
        IconButton(onClick = onSendClick) {
            Image(
                painter = painterResource(id = R.drawable.ic_send), // ✈️ 아이콘 리소스
                contentDescription = "Send",
            )
        }
    }
}


@Composable
fun ChatMessage(
    text: String,
    isUser: Boolean,
    isContinuous: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isContinuous) {
            Spacer(Modifier.height(4.dp))
        }
        Surface(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
            color = if (isUser) AppColors.Primary600 else AppColors.Primary100,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BackgroundColors.Background300)

        ) {
            Text(
                text = text,
                color = if (isUser) BackgroundColors.Background50 else BackgroundColors.Background700,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}
