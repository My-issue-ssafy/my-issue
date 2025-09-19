package com.ioi.myssue.designsystem.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ioi.myssue.designsystem.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyssueBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        containerColor = AppColors.Primary300,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = AppColors.Primary400,
                width = 80.dp
            )
        },
        modifier = Modifier.fillMaxSize().systemBarsPadding()
    ) {
        content()
    }
}