package com.ioi.myssue.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ioi.myssue.R
import com.ioi.myssue.ui.main.MainActivity

object Noti {
    private const val CHANNEL_ID = "news"

    fun ensureChannel(ctx: Context) {
        val mgr = ctx.getSystemService(NotificationManager::class.java)
        val ch =
            NotificationChannel(CHANNEL_ID, "뉴스 알림", NotificationManager.IMPORTANCE_DEFAULT)
        mgr.createNotificationChannel(ch)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun show(ctx: Context, title: String, body: String, data: Map<String, String>) {
        ensureChannel(ctx)
        val intent = Intent(ctx, MainActivity::class.java)
            .putExtra("push_route", data["route"])
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val noti = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        NotificationManagerCompat.from(ctx).notify((System.currentTimeMillis()%100000).toInt(), noti)
    }
}
