package com.ioi.myssue.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role

fun Modifier.clickableNoRipple(
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "clickableNoRipple"
        properties["onClick"] = onClick
    }
) {
    val interaction = remember { MutableInteractionSource() }
    this
        .indication(interaction, null)
        .clickable(
            interactionSource = interaction,
            indication = null,
            role = Role.Button,
            onClick = onClick
        )
}
