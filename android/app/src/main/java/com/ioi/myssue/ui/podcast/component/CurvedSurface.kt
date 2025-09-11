package com.ioi.myssue.ui.podcast.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.designsystem.theme.BackgroundColors

@Composable
fun CurvedSurface(
    modifier: Modifier = Modifier,
    backgroundColor: Color = BackgroundColors.Background100,
    containerGradient: List<Color> = listOf(AppColors.Primary400, AppColors.Primary500),
    curveHeight: Dp = 100.dp,
    leftYRatio: Float = 0.0f,
    rightYRatio: Float = 1f,
    c1: Pair<Float, Float> = 0.15f to 1f,
    c2: Pair<Float, Float> = 0.85f to 0.00f,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    val curveHeightPx = with(density) { curveHeight.toPx() }

    val rightY = curveHeightPx * rightYRatio
    val contentTopPadding = with(density) { rightY.toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(containerGradient)
            )
    ) {
        Surface(
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(curveHeight)
                ) {
                    drawCurvedHeader(
                        bg = backgroundColor,
                        leftYRatio = leftYRatio,
                        rightYRatio = rightYRatio,
                        c1 = c1, c2 = c2
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = contentTopPadding / 2 + contentPadding,
                            start = contentPadding,
                            end = contentPadding,
                            bottom = contentPadding
                        ),
                    content = content
                )
            }
        }
    }
}

private fun DrawScope.drawCurvedHeader(
    bg: Color,
    leftYRatio: Float,
    rightYRatio: Float,
    c1: Pair<Float, Float>,
    c2: Pair<Float, Float>,
) {
    val w = size.width
    val h = size.height

    // 베지어 포인트들
    val p0 = Offset(0f, h * leftYRatio)            // 좌측 시작 (높게)
    val p3 = Offset(w, h * rightYRatio)            // 우측 끝 (낮게)
    val cp1 = Offset(w * c1.first, h * c1.second)  // 컨트롤1
    val cp2 = Offset(w * c2.first, h * c2.second)  // 컨트롤2

    // 곡선 위쪽 영역을 채우는 path (상단을 꽉 채우며 곡선으로 닫힘)
    val path = Path().apply {
        moveTo(0f, 0f)
        lineTo(p0.x, p0.y)
        cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p3.x, p3.y)
        lineTo(w, 0f)
        close()
    }

    drawPath(path = path, color = bg)
}
