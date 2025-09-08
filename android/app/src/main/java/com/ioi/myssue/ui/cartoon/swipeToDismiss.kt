import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

enum class SwipeDir { Left, Right }

fun Modifier.swipeWithAnimation(
    key: Any? = null,
    thresholdFraction: Float = 0.35f,
    exitDistance: Dp = 600.dp,
    maxRotation: Float = 20f,
    locked: Boolean = false,
    onSwiped: (SwipeDir) -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val exitPx = with(density) { exitDistance.toPx() }

    var widthPx by remember { mutableIntStateOf(1) }
    val tx = remember(key) { Animatable(0f) }
    val rot = remember(key) { Animatable(0f) }

    LaunchedEffect(locked) {
        if (locked) {
            tx.stop(); rot.stop()
            tx.snapTo(0f); rot.snapTo(0f)
        }
    }

    this
        .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
        .pointerInput(key, widthPx, locked) {
            if (locked) return@pointerInput
            detectDragGestures(
                onDrag = { change, drag ->
                    change.consume()
                    val newX = tx.value + drag.x
                    scope.launch { tx.snapTo(newX) }
                    scope.launch {
                        val r = (newX / widthPx) * maxRotation
                        rot.snapTo((-r).coerceIn(-maxRotation, maxRotation))
                    }
                },
                onDragEnd = {
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
                            tx.animateTo(target, tween(280))
                            rot.animateTo(endRot, tween(280))
                            onSwiped(dir)
                        }
                    }
                },
                onDragCancel = {
                    scope.launch {
                        tx.animateTo(0f, tween(200))
                        rot.animateTo(0f, tween(200))
                    }
                }
            )
        }
        .graphicsLayer {
            translationX = if (locked) 0f else tx.value
            rotationZ = if (locked) 0f else rot.value
        }
}
