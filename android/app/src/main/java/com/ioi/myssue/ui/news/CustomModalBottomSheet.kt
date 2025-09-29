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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.annotations.concurrent.Background
import com.ioi.myssue.designsystem.theme.AppColors.Primary600
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
@SuppressLint("ConfigurationScreenWidthHeight", "UnrememberedMutableState")
@Composable
fun CustomModalBottomSheetDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    shape: Shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
    background: Brush? = null,
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

    var isDismissing by remember { mutableStateOf(false) }

    // 닫기 애니메이션 + dismiss 래퍼
    fun animateAndDismiss() {
        if (isDismissing) return // ✅ 이미 실행 중이면 무시
        isDismissing = true

        scope.launch {
            offsetY.animateTo(
                targetValue = screenHeightPx,
                animationSpec = tween(300)
            )
            onDismiss()
            isDismissing = false // ✅ 필요하다면 reset
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
                    .statusBarsPadding()
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .then(
                        if(background == null) {
                            Modifier.background(color, shape)
                        } else {
                            Modifier.background(background, shape)
                        }
                    )

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
                        .navigationBarsPadding()
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
fun SheetDragHandle(color: Color = Primary600) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(20.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(width = 80.dp, height = 5.dp)
                .background(
                    color = color,
                    shape = CircleShape
                )
        )
    }
}