package com.ioi.myssue.ui.cartoon

import android.R.attr.rotationY
import android.R.attr.translationX
import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.ioi.myssue.LocalAnalytics
import com.ioi.myssue.designsystem.theme.BackgroundColors.Background50
import com.ioi.myssue.domain.model.CartoonNews
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CartoonCard(
    modifier: Modifier = Modifier,
    cartoon: CartoonNews,
    isExiting: Boolean,
    exitDir: Int,
    isSmallMode: Boolean = false,
    isFlippable: Boolean = true,
    onExitEnd: (() -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    val animKey = remember { "${cartoon.toonImageUrl}|${cartoon.newsTitle}" }
    val tx = remember(animKey) { Animatable(0f) }
    val rotZ = remember(animKey) { Animatable(0f) }

    var flipped by rememberSaveable(animKey) { mutableStateOf(false) }
    val flip by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(600),
        label = "flip"
    )
    val showFront = flip <= 90f

    val analyticsLogger = LocalAnalytics.current

    LaunchedEffect(flipped) {
        if(flipped) {
            analyticsLogger.logToonClick(cartoon.newsId)
        }
    }

    LaunchedEffect(isExiting) {
        if (isExiting) {
            coroutineScope {
                launch { tx.animateTo(600f * exitDir, tween(600)) }
                launch { rotZ.animateTo(-30f * exitDir, tween(600)) }
            }
            onExitEnd?.invoke()
        } else {
            tx.snapTo(0f); rotZ.snapTo(0f)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val cameraDistancePx = with(LocalDensity.current) { 12.dp.toPx() }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                if (isExiting) {
                    translationX = tx.value
                    rotationZ = rotZ.value
                }
                rotationY = flip
                cameraDistance = cameraDistancePx
            }
            .clickable(
                enabled = !isExiting,
                indication = null,
                interactionSource = interactionSource
            ) {
                onClick()
                if(isFlippable) {
                    flipped = !flipped
                }
              },
        contentAlignment = Alignment.Center
    ) {
        Card(
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Background50),
        ) {
            if (showFront) {
                CartoonImage(
                    url = cartoon.toonImageUrl,
                    contentDesc = cartoon.newsTitle,
                    modifier = Modifier
                        .padding(4.dp)
                        .align(Alignment.CenterHorizontally)
                )
            } else {
                Box {
                    CartoonImage(
                        url = cartoon.toonImageUrl,
                        contentDesc = cartoon.newsTitle,
                        modifier = Modifier
                            .padding(4.dp)
                            .alpha(0.1f)
                            .graphicsLayer { rotationY = 180f }
                    )
                    CartoonWithNewsSummary(cartoon = cartoon, scale = if(isSmallMode) 0.5f else 1.0f)
                }
            }
        }
    }
}

@Composable
fun ExpandedCartoonCard(
    modifier: Modifier = Modifier,
    cartoon: CartoonNews,
    isExiting: Boolean,
    exitDir: Int,
    scale: Float = 1.0f,
    isFlippable: Boolean = true,
    onExitEnd: (() -> Unit)? = null,
    onClick: () -> Unit = {},
    expandedContent : @Composable () -> Unit
) {
    val appearAlpha = remember { Animatable(0f) }
    val appearScale = remember { Animatable(0.8f) }

    LaunchedEffect(cartoon) {
        appearAlpha.snapTo(0f)
        appearScale.snapTo(0.8f)
        launch { appearAlpha.animateTo(1f, tween(500)) }
        launch { appearScale.animateTo(1f, tween(500)) }
    }

    val animKey = remember { "${cartoon.toonImageUrl}|${cartoon.newsTitle}" }
    val tx = remember(animKey) { Animatable(0f) }
    val rotZ = remember(animKey) { Animatable(0f) }

    var flipped by rememberSaveable(animKey) { mutableStateOf(false) }
    val flip by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(600),
        label = "flip"
    )
    val showFront = flip <= 90f

    LaunchedEffect(isExiting) {
        if (isExiting) {
            coroutineScope {
                launch { tx.animateTo(600f * exitDir, tween(600)) }
                launch { rotZ.animateTo(-30f * exitDir, tween(600)) }
            }
            onExitEnd?.invoke()
        } else {
            tx.snapTo(0f); rotZ.snapTo(0f)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val cameraDistancePx = with(LocalDensity.current) { 12.dp.toPx() }

    Box(
        modifier = modifier
            .padding(8.dp)
            .graphicsLayer {
                alpha = appearAlpha.value
                scaleX = appearScale.value
                scaleY = appearScale.value

                if (isExiting) {
                    translationX = tx.value
                    rotationZ = rotZ.value
                }
                rotationY = flip
                cameraDistance = cameraDistancePx
            }
            .clickable(
                enabled = !isExiting,
                indication = null,
                interactionSource = interactionSource
            ) {
                onClick()
                if (isFlippable) flipped = !flipped
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Background50),
        ) {
            if (showFront) {
                CartoonImage(
                    url = cartoon.toonImageUrl,
                    contentDesc = cartoon.newsTitle,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.CenterHorizontally)
                )
            } else {
                Box {
                    CartoonImage(
                        url = cartoon.toonImageUrl,
                        contentDesc = cartoon.newsTitle,
                        modifier = Modifier
                            .padding(8.dp)
                            .alpha(0.1f)
                            .graphicsLayer { rotationY = 180f }
                    )
                    CartoonWithNewsSummary(cartoon = cartoon, scale = scale)
                }
            }
            expandedContent()
        }
    }
}


@Composable
private fun CartoonWithNewsSummary(
    cartoon: CartoonNews,
    scale: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .graphicsLayer { rotationY = 180f },
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.padding(4.dp),
            text = cartoon.newsTitle,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 32.sp * scale,
                lineHeight = 44.sp * scale,
            ),
            maxLines = if(scale < 1f) 5 else 3,
            overflow = TextOverflow.Ellipsis
        )

        if(scale < 1f) return@Column
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            color = Background50,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = cartoon.newsDescription,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp * scale
                ),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
}

@Composable
private fun CartoonImage(
    url: String,
    contentDesc: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val request = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .size(Size.ORIGINAL)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDesc,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Fit,

    )
}