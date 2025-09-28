package com.ioi.myssue.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@Composable
fun NotificationPermission(
    firstLaunch: Boolean,
    onNewGranted: () -> Unit = {},
    onRefused: () -> Unit = {},
    onAlreadyGranted: () -> Unit = {}
) {
    val context = LocalContext.current

    // 33 미만은 권한 필요 없음
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) { onAlreadyGranted() }
        return
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onNewGranted() else onRefused()
    }

    LaunchedEffect(firstLaunch) {
        if (!firstLaunch) return@LaunchedEffect

        val perm = Manifest.permission.POST_NOTIFICATIONS
        val granted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            onAlreadyGranted()
        } else {
            launcher.launch(perm)
        }
    }
}

private fun Context.findActivity(): Activity =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> error("Activity not found")
    }
