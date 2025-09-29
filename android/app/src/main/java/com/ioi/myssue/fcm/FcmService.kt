package com.ioi.myssue.fcm

import android.Manifest
import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(msg: RemoteMessage) {
        val title = msg.notification?.title ?: msg.data["title"] ?: "알림"
        val body = msg.notification?.body ?: msg.data["body"] ?: ""
        Noti.show(applicationContext, title, body, msg.data)
    }
}
