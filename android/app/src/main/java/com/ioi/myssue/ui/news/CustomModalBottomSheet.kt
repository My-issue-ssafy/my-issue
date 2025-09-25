package com.ioi.myssue.ui.news

import android.annotation.SuppressLint
import android.util.Log
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ioi.myssue.designsystem.theme.AppColors.Primary600
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
@SuppressLint("ConfigurationScreenWidthHeight", "UnrememberedMutableState")
@Composable
fun CustomModalBottomSheetDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit = { SheetDragHandle() },
    headerContent: @Composable () -> Unit = {},
    bodyContent: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val density = LocalDensity.current

    val screenHeightPx = with(density) { screenHeight.toPx() }
    val offsetY = remember { androidx.compose.animation.core.Animatable(screenHeightPx) }

    // 열릴 때 슬라이드 애니메이션
    LaunchedEffect(Unit) {
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400)
        )
    }

    // 닫기 애니메이션 + dismiss 래퍼
    fun animateAndDismiss() {
        scope.launch {
            offsetY.animateTo(
                targetValue = screenHeightPx,
                animationSpec = tween(300)
            )
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { animateAndDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // ✅ 가로 폭 꽉 차게
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .background(Color.White, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            ) {
                /** ───── Header (드래그 전용 + 핸들 표시) ───── */
                Column(
                    Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newOffset = (offsetY.value + dragAmount.y).coerceAtLeast(0f)
                                    scope.launch { offsetY.snapTo(newOffset) }
                                },
                                onDragEnd = {
                                    val threshold = screenHeightPx * 0.15f
                                    scope.launch {
                                        if (offsetY.value > threshold) {
                                            animateAndDismiss()
                                        } else {
                                            offsetY.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.6f,
                                                    stiffness = 300f
                                                )
                                            )
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    dragHandle()
                    headerContent()
                    Spacer(Modifier.height(12.dp))
                }

                /** ───── Body (스크롤 전용) ───── */
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    bodyContent()
                }
            }
        }
    }
}

// 시트 핸들
@Composable
fun SheetDragHandle() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(width = 80.dp, height = 5.dp)
                .background(
                    color = Primary600,
                    shape = CircleShape
                )
        )
    }
}