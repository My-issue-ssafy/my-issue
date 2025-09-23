package com.ioi.myssue.ui.common

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class SwipeDir { Left, Right, Up }

fun Modifier.swipeWithAnimation(
    key: Any? = null,
    thresholdFraction: Float = 0.35f,
    exitDistance: Dp = 600.dp,
    maxRotation: Float = 20f,
    locked: Boolean = false,
    upDragFactor: Float = 0.4f, // 위로 들릴 때 반영 비율
    onSwiped: (SwipeDir) -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val exitPx = with(density) { exitDistance.toPx() }

    var widthPx by remember { mutableIntStateOf(1) }
    val tx = remember(key) { Animatable(0f) }
    val ty = remember(key) { Animatable(0f) }
    val rot = remember(key) { Animatable(0f) }

    var totalDx by remember { mutableIntStateOf(0) }
    var totalDy by remember { mutableIntStateOf(0) }
    var swipeAxis by remember { mutableStateOf<SwipeDir?>(null) }

    LaunchedEffect(locked) {
        if (locked) {
            tx.stop(); rot.stop(); ty.stop()
            tx.snapTo(0f); rot.snapTo(0f); ty.snapTo(0f)
        }
    }

    this
        .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
        .pointerInput(key, widthPx, locked) {
            if (locked) return@pointerInput
            detectDragGestures(
                onDragStart = {
                    totalDx = 0; totalDy = 0
                    swipeAxis = null
                },
                onDrag = { change, drag ->
                    change.consume()
                    totalDx += drag.x.toInt()
                    totalDy += drag.y.toInt()

                    // 첫 움직임에서 축 고정
                    if (swipeAxis == null) {
                        swipeAxis = if (abs(totalDx) > abs(totalDy)) SwipeDir.Left else SwipeDir.Up
                    }

                    if (swipeAxis == SwipeDir.Left) {
                        val newX = tx.value + drag.x
                        scope.launch { tx.snapTo(newX) }
                        scope.launch {
                            val r = (newX / widthPx) * maxRotation
                            rot.snapTo((-r).coerceIn(-maxRotation, maxRotation))
                        }
                    } else if (swipeAxis == SwipeDir.Up) {
                        // 위로는 일부만 반영 (20%)
                        val newY = ty.value + drag.y * upDragFactor
                        scope.launch { ty.snapTo(newY.coerceAtMost(0f)) }
                    }
                },
                onDragEnd = {
                    if (swipeAxis == SwipeDir.Up) {
                        val absDx = abs(totalDx)
                        val absDy = abs(totalDy)

                        // ⬆️ 위로 스와이프 동작 판정 : 세로 이동이 크고, 위로 충분히 이동했을 때
                        if (absDy > absDx && totalDy < -100) {
                            onSwiped(SwipeDir.Up)
                        }

                        // UI는 항상 원래 위치로 복귀
                        scope.launch { ty.animateTo(0f, tween(250)) }
                        return@detectDragGestures
                    }

                    if (swipeAxis == SwipeDir.Left) {
                        // 기존 좌우 스와이프 처리 그대로
                        val threshold = widthPx * thresholdFraction
                        val x = tx.value
                        val dir = when {
                            x > threshold -> SwipeDir.Right
                            x < -threshold -> SwipeDir.Left
                            else -> null
                        }
                        if (dir == null) {
                            scope.launch {
                                tx.animateTo(0f, tween(250))
                                rot.animateTo(0f, tween(250))
                            }
                        } else {
                            val target = if (dir == SwipeDir.Right) x + exitPx else x - exitPx
                            val endRot = if (dir == SwipeDir.Right) -maxRotation else maxRotation
                            scope.launch {
                                onSwiped(dir)
                                tx.animateTo(target, tween(280))
                                rot.animateTo(endRot, tween(280))
                            }
                        }
                    }
                },
                onDragCancel = {
                    scope.launch {
                        tx.animateTo(0f, tween(200))
                        rot.animateTo(0f, tween(200))
                        ty.animateTo(0f, tween(200))
                    }
                }
            )
        }
        .graphicsLayer {
            translationX = if (locked) 0f else tx.value
            translationY = if (locked) 0f else ty.value
            rotationZ = if (locked) 0f else rot.value
        }
}
