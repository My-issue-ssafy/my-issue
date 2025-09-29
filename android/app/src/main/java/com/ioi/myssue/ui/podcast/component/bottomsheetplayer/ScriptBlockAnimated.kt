package com.ioi.myssue.ui.podcast.component.bottomsheetplayer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.ioi.myssue.domain.model.ScriptLine
import com.ioi.myssue.ui.common.clickableNoRipple
import kotlinx.coroutines.delay

@Composable
fun ColumnScope.ScriptBlockAnimated(
    scripts: List<ScriptLine>,
    currentIndex: Int,
    onLineClick: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    var isAutoScrollEnabled by remember { mutableStateOf(true) }
    var lastUserInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (System.currentTimeMillis() - lastUserInteractionTime > 5000) {
                isAutoScrollEnabled = true
            }
        }
    }

    LaunchedEffect(currentIndex, isAutoScrollEnabled) {
        if (isAutoScrollEnabled && currentIndex >= 0) {
            val viewportHeight =
                listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset

            val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == currentIndex }
            val itemHeight =
                itemInfo?.size ?: (listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0)

            val centerOffset = (viewportHeight / 2) - itemHeight

            listState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = -centerOffset
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        lastUserInteractionTime = System.currentTimeMillis()
                        isAutoScrollEnabled = false
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(scripts) { index, line ->
            val distance = kotlin.math.abs(index - currentIndex)
            val styleTarget = when (distance) {
                0 -> 1f
                1 -> 0.5f
                else -> 0f
            }

            val style by animateFloatAsState(
                targetValue = styleTarget,
                animationSpec = tween(400),
                label = "style-$index"
            )

            ScriptLineItem(
                line = line,
                style = style,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickableNoRipple {
                        onLineClick(index)
                    }
            )
        }
    }
}

