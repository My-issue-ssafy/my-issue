package com.ioi.myssue.ui.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter.State.Empty.painter
import coil.compose.rememberAsyncImagePainter
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.designsystem.theme.Pink
import com.ioi.myssue.domain.model.Notification
import com.ioi.myssue.ui.common.clickableNoRipple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val TAG = "NotificationComponent"

@Composable
fun NotificationTopHeader(
    title: String = "최근 알림",
    isClearAllEnabled: Boolean = true,
    onClickClearAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColors.Background100)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        if (onClickClearAll != null) {
            DeleteAllButton(
                enabled = isClearAllEnabled,
                onClick = onClickClearAll
            )
        }
    }
}

@Composable
private fun DeleteAllButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val container = if (enabled) BackgroundColors.Background250 else AppColors.Primary450
    val content = if (enabled) AppColors.Primary600 else BackgroundColors.Background50

    Surface(
        color = container,
        shape = CircleShape
    ) {
        val clickableMod = if (enabled) {
            Modifier
                .clip(CircleShape)
                .clickable(onClick = onClick)
        } else Modifier

        Text(
            text = "전체 삭제",
            style = MaterialTheme.typography.bodyLarge,
            color = content,
            modifier = clickableMod
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun NotificationDateHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = BackgroundColors.Background500
        )
    }
}

@Composable
fun NotificationItem(
    n: Notification,
    timeText: String,
    onClick: () -> Unit
) {
    val isRead = n.read

    val containerColor =
        if (isRead) BackgroundColors.Background100 else AppColors.Primary150

    val textColor =
        if (isRead) BackgroundColors.Background400
        else BackgroundColors.Background700

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 썸네일
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color(0xFFFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (n.thumbnail != null) {
                AsyncImage(
                    model = n.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = rememberAsyncImagePainter(R.drawable.ic_empty_thumbnail),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = n.content,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = textColor,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(2.dp))

        Box(
            modifier = Modifier
                .widthIn(min = 64.dp)
                .padding(bottom = 6.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
            )
        }
    }
}

@Composable
fun SwipeableNotificationItem(
    n: Notification,
    timeText: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    capWidth: Dp = 60.dp,            // 최대 스와이프 길이
    thresholdFraction: Float = 1.0f, // cap의 몇 % 이상에서 손 떼면 삭제
    exitDurationMs: Int = 240,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val capPx = with(density) { capWidth.toPx() }
    val thresholdPx = capPx * thresholdFraction

    val offsetX = remember { Animatable(0f) } // 음수: 왼쪽
    var isRemoving by remember { mutableStateOf(false) }

    LaunchedEffect(isRemoving) {
        if (isRemoving) {
            delay(exitDurationMs.toLong())
            onDelete()
        }
    }

    AnimatedVisibility(
        visible = !isRemoving,
        exit = shrinkVertically(tween(exitDurationMs), shrinkTowards = Alignment.Top)
                + fadeOut(tween(exitDurationMs))
    ) {
        Box(
            modifier
                .fillMaxWidth()
                .background(Pink)
                .heightIn(min = 72.dp)
        ) {
            // 오른쪽 고정 액션 영역
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(capWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_trash),
                    contentDescription = "삭제",
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .graphicsLayer { translationX = offsetX.value }
                    .pointerInput(capPx, thresholdPx) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                val proposed = offsetX.value + dragAmount
                                val clamped = proposed.coerceIn(-capPx, 0f)
                                scope.launch { offsetX.snapTo(clamped) }
                                change.consume() // 수평 제스처 소비
                            },
                            onDragEnd = {
                                if (abs(offsetX.value) >= thresholdPx) {
                                    scope.launch {
                                        offsetX.animateTo(
                                            -capPx,
                                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                        )
                                        isRemoving = true
                                    }
                                } else {
                                    // 원위치로 복귀
                                    scope.launch {
                                        offsetX.animateTo(
                                            0f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    offsetX.animateTo(
                                        0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        )
                    }
                    .fillMaxWidth()
            ) {
                NotificationItem(n = n, timeText = timeText, onClick = onClick)
            }
        }
    }
}

@Composable
fun NotificationEmpty(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(R.drawable.ic_empty_notification),
            contentDescription = null,
            modifier = Modifier.size(220.dp)
        )
        Text(
            text = "새로운 소식이 도착하면",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = BackgroundColors.Background700,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "알려드릴게요",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = BackgroundColors.Background700,
        )
    }
}