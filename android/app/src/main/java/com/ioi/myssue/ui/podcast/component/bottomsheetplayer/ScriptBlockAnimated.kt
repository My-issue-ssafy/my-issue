package com.ioi.myssue.ui.podcast.component.bottomsheetplayer

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ioi.myssue.ui.podcast.ScriptLine

@Composable
fun ColumnScope.ScriptBlockAnimated(scripts: List<ScriptLine>, currentIndex: Int) {
    val slotHeight = with(LocalDensity.current) { 72.dp.toPx() }
    val transition = updateTransition(targetState = currentIndex, label = "scriptTransition")
    val window = (currentIndex - 2)..(currentIndex + 2)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        scripts.forEachIndexed { index, line ->
            if (index in window) {
                val offsetY by rememberLineAnimations(
                    transition = transition,
                    index = index,
                    slotHeight = slotHeight
                )
                val style by rememberLineStyle(transition, index)

                ScriptLineItem(
                    line = line,
                    offsetY = offsetY,
                    style = style
                )
            }
        }
    }
}

/**
 * 1. 라인 위치 애니메이션
 */
@Composable
private fun rememberLineAnimations(
    transition: Transition<Int>,
    index: Int,
    slotHeight: Float
) = transition.animateFloat(
    transitionSpec = {
        tween(
            400,
            easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        )
    },
    label = "offsetY-$index"
) { target -> (index - target) * slotHeight }

/**
 * 2. 라인 스타일 애니메이션 값 (중앙일수록 강조)
 */
@Composable
private fun rememberLineStyle(
    transition: Transition<Int>,
    index: Int
) = transition.animateFloat(
    transitionSpec = { tween(400) },
    label = "style-$index"
) { target ->
    when (index - target) {
        -2, +2 -> 0f
        -1, +1 -> 0.5f
        0 -> 1f
        else -> 0f
    }
}
