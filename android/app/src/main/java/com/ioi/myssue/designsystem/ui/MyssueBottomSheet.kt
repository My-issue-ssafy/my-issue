package com.ioi.myssue.designsystem.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.ui.news.CustomModalBottomSheetDialog
import com.ioi.myssue.ui.news.SheetDragHandle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyssueBottomSheet(
    onDismissRequest: () -> Unit = {},
    headerContent: @Composable () -> Unit,
    bodyContent: @Composable () -> Unit,
) {
    CustomModalBottomSheetDialog(
        onDismiss = onDismissRequest,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        color = AppColors.Primary300,
        dragHandle = {
            SheetDragHandle(AppColors.Primary400)
        },
        background = Brush.verticalGradient(listOf(AppColors.Primary300, AppColors.Primary500)),
        headerContent = headerContent
    ) {
        bodyContent()
    }
}