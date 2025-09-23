package com.ioi.myssue.ui.podcast.component.bottomsheetplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ioi.myssue.designsystem.theme.BackgroundColors
import com.ioi.myssue.domain.model.ScriptLine

@Composable
fun ScriptLineItem(
    modifier: Modifier = Modifier,
    line: ScriptLine,
    style: Float
) {
    val blur: Dp = (2 * (1f - style)).dp
    val alpha = 0.3f + 0.7f * style
    val shadowAlpha = 0.4f * style
    val shadowRadius = 12f * style
    val shadowOffset = 4f * style
    val scale = 0.8f + 0.2f * style

    Row(
        modifier = modifier
            .padding(vertical = 20.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                transformOrigin = if (line.isLeftSpeaker) {
                    TransformOrigin(0f, 0.5f)
                } else {
                    TransformOrigin(1f, 0.5f)
                }
            },
        horizontalArrangement = if (line.isLeftSpeaker) Arrangement.Start else Arrangement.End
    ) {
        AnimatedScriptText(line, blur, shadowAlpha, shadowOffset, shadowRadius)
    }
}

@Composable
private fun AnimatedScriptText(
    line: ScriptLine,
    blur: Dp,
    shadowAlpha: Float,
    shadowOffset: Float,
    shadowRadius: Float
) {
    Text(
        text = line.line,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .blur(blur),
        color = BackgroundColors.Background50,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            shadow = Shadow(
                color = Color.Black.copy(alpha = shadowAlpha),
                offset = Offset(shadowOffset, shadowOffset),
                blurRadius = shadowRadius
            )
        ),
        textAlign = if (line.isLeftSpeaker) TextAlign.Start else TextAlign.End
    )
}
