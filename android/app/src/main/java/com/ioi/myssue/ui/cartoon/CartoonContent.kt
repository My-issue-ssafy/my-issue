package com.ioi.myssue.ui.cartoon

import android.util.Log
import com.ioi.myssue.ui.common.SwipeDir
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.ioi.myssue.LocalAnalytics
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.BackgroundColors.Background100
import com.ioi.myssue.designsystem.theme.BackgroundColors.Background50
import com.ioi.myssue.domain.model.CartoonNews
import com.ioi.myssue.ui.common.swipeWithAnimation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun CartoonActionButtons(
    isLikePressed: Boolean,
    isHatePressed: Boolean,
    canInteract: Boolean,
    onLikePressed: () -> Unit,
    onHatePressed: () -> Unit
) {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ActionButton(
            normalDrawable = R.drawable.ic_toon_hate,
            pressedDrawable = R.drawable.ic_toon_hate_pressed,
            isPressed = isHatePressed,
            enabled = canInteract,
            onClick = onHatePressed
        )
        Spacer(Modifier.weight(1f))
        ActionButton(
            normalDrawable = R.drawable.ic_toon_like,
            pressedDrawable = R.drawable.ic_toon_like_pressed,
            isPressed = isLikePressed,
            enabled = canInteract,
            onClick = onLikePressed
        )
    }
}

@Composable
private fun ActionButton(
    normalDrawable: Int,
    pressedDrawable: Int,
    isPressed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val currentDrawable = if (isPressed) pressedDrawable else normalDrawable
    Image(
        painter = painterResource(currentDrawable),
        contentDescription = null,
        modifier = Modifier
            .size(80.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                enabled = enabled,
                onClick = onClick
            )
    )
}

@Composable
fun CartoonCardStack(
    cartoonList: List<CartoonNews>,
    currentIndex: Int,
    exitTrigger: Int,
    isSwiping: Boolean,
    onExitFinished: () -> Unit,
    onLikePressed: (Boolean) -> Unit,
    onHatePressed: (Boolean) -> Unit,
    onShowDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var exitingKey by remember { mutableStateOf<String?>(null) }
    var exitDir by remember { mutableIntStateOf(1) }

    fun keyOf(item: CartoonNews) = "${item.toonImageUrl}|${item.newsTitle}"

    LaunchedEffect(exitTrigger) {
        if (exitTrigger != 0 && currentIndex in cartoonList.indices) {
            exitingKey = keyOf(cartoonList[currentIndex])
            exitDir = if (exitTrigger > 0) 1 else -1
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        for (i in cartoonList.size - 1 downTo currentIndex) {
            val item = cartoonList[i]
            val isTop = i == currentIndex
            val isExiting = isTop && (keyOf(item) == exitingKey)
            val layerOrder = i - currentIndex

            CartoonCard(
                cartoon = item,
                isExiting = isExiting,
                exitDir = exitDir,
                modifier = Modifier
                    .zIndex(if (isExiting) 1f else -layerOrder.toFloat())
                    .swipeWithAnimation(
                        key = item.toonImageUrl,
                        locked = isExiting && !isSwiping,
                        onSwiped = { dir ->
                            when (dir) {
                                SwipeDir.Left -> onHatePressed(true)
                                SwipeDir.Right -> onLikePressed(true)
                                SwipeDir.Up -> onShowDetail(item.newsId)
                            }
                        }
                    ),
                onExitEnd = {
                    if (isExiting) {
                        exitingKey = null
                        onExitFinished()
                    }
                }
            )
        }
    }
}

@Composable
private fun CartoonCard(
    cartoon: CartoonNews,
    isExiting: Boolean,
    exitDir: Int,
    modifier: Modifier = Modifier,
    onExitEnd: (() -> Unit)? = null
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

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = modifier
                .size(maxWidth - 32.dp)
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
                ) { flipped = !flipped }
        ) {
            Card(
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Background100),
                modifier = Modifier.fillMaxSize()
            ) {
                if (showFront) {
                    CartoonImage(
                        url = cartoon.toonImageUrl,
                        contentDesc = cartoon.newsTitle,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                } else {
                    Box {
                        CartoonImage(
                            url = cartoon.toonImageUrl,
                            contentDesc = cartoon.newsTitle,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .alpha(0.1f)
                                .graphicsLayer { rotationY = 180f }
                        )
                        CartoonWithNewsSummary(cartoon)
                    }
                }
            }
        }
    }

}

@Composable
private fun CartoonWithNewsSummary(cartoon: CartoonNews) {
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
            style = MaterialTheme.typography.titleLarge,
            fontSize = 32.sp
        )

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
                style = MaterialTheme.typography.titleMedium
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
        modifier = modifier.fillMaxWidth(),
        contentScale = ContentScale.Fit
    )
}

@Preview(showBackground = true)
@Composable
fun CartoonScreenPreview() {
    MaterialTheme {
        CartoonScreen()
    }
}
