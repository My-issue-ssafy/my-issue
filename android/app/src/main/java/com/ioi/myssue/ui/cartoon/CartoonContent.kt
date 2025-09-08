package com.ioi.myssue.ui.cartoon

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.ioi.myssue.R

@Composable
fun CartoonActionButtons(
    isLikePressed: Boolean,
    isHatePressed: Boolean,
    canInteract: Boolean,
    onLikePressed: () -> Unit,
    onHatePressed: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
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
    onExitFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var exitingKey by remember { mutableStateOf<String?>(null) }
    var exitDir by remember { mutableIntStateOf(1) }

    fun keyOf(item: CartoonNews) = "${item.cartoonUrl}|${item.newsTitle}"

    LaunchedEffect(exitTrigger) {
        if (exitTrigger != 0 && currentIndex in cartoonList.indices) {
            exitingKey = keyOf(cartoonList[currentIndex])
            exitDir = if (exitTrigger > 0) 1 else -1
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val baseStart = currentIndex

        for (i in cartoonList.size - 1 downTo baseStart) {
            val item = cartoonList[i]
            val isTop = i == baseStart
            val isExiting = isTop && (keyOf(item) == exitingKey)
            val layerOrder = i - baseStart

            key(keyOf(item)) {
                CartoonCard(
                    cartoon = item,
                    isExiting = isExiting,
                    exitDir = exitDir,
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isExiting) 1f else -layerOrder.toFloat()),
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
}

@Composable
private fun CartoonCard(
    cartoon: CartoonNews,
    isExiting: Boolean,
    exitDir: Int,
    modifier: Modifier = Modifier,
    onExitEnd: (() -> Unit)? = null
) {
    val animKey = remember { "${cartoon.cartoonUrl}|${cartoon.newsTitle}" }
    val tx = remember(animKey) { Animatable(0f) }
    val rot = remember(animKey) { Animatable(0f) }

    LaunchedEffect(isExiting) {
        if (isExiting) {
            coroutineScope {
                launch { tx.animateTo(600f * exitDir, tween(600)) }
                launch { rot.animateTo(-30f * exitDir, tween(600)) }
            }
            onExitEnd?.invoke()
        } else {
            tx.snapTo(0f)
            rot.snapTo(0f)
        }
    }

    CartoonImage(
        url = cartoon.cartoonUrl,
        contentDesc = cartoon.newsTitle,
        modifier = modifier.graphicsLayer {
            if (isExiting) {
                translationX = tx.value
                rotationZ = rot.value
                compositingStrategy = CompositingStrategy.Offscreen
            }
        }
    )
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
