package com.ioi.myssue.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ioi.myssue.R

import kotlin.run
import androidx.core.net.toUri

object Noti {
    private const val CHANNEL_ID = "news_high"

    fun ensureChannel(ctx: Context) {
        val mgr = ctx.getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "뉴스 알림(팝업)",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "개인화 뉴스 알림"
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            mgr.createNotificationChannel(ch)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun show(ctx: Context, title: String, body: String, data: Map<String, String>) {
        ensureChannel(ctx)

        val newsId = data["newsId"] ?: return
        val thumbUrl = data["thumbnailUrl"]

        val clickIntent = Intent(
            Intent.ACTION_VIEW,
            "myssue://news/$newsId".toUri()
        ).apply {
            `package` = ctx.packageName
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val requestCode = newsId.hashCode()
        val pi = PendingIntent.getActivity(
            ctx, requestCode, clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // 썸네일 스타일
        val bitmap = thumbUrl?.let { safeLoadBitmap(it) }
        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null as Bitmap?)
                )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        NotificationManagerCompat.from(ctx).notify(requestCode, builder.build())
    }

    private fun safeLoadBitmap(urlStr: String): Bitmap? = runCatching {
        val url = java.net.URL(urlStr)
        (url.openConnection() as java.net.HttpURLConnection).run {
            connectTimeout = 4000
            readTimeout = 4000
            doInput = true
            instanceFollowRedirects = true
            connect()
            inputStream.use { stream ->
                val bytes = stream.readBytes()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val target = 1024
                var sample = 1
                while (bounds.outWidth / sample > target || bounds.outHeight / sample > target) {
                    sample *= 2
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            }
        }
    }.getOrNull()
}
