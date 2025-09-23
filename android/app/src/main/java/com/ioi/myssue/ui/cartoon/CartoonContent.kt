package com.ioi.myssue.ui.cartoon

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ioi.myssue.R
import com.ioi.myssue.domain.model.CartoonNews
import com.ioi.myssue.ui.common.SwipeDir
import com.ioi.myssue.ui.common.swipeWithAnimation

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
                    .padding(16.dp)
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


@Preview(showBackground = true)
@Composable
fun CartoonScreenPreview() {
    MaterialTheme {
        CartoonScreen()
    }
}
