package com.phamtunglam.lamity.downloader.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.ForegroundInfo

/** Foreground-service progress notification for one download. */
internal class DownloadNotification(
    private val context: Context,
    private val notificationId: Int,
) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun foregroundInfo(title: String, progressPercent: Int): ForegroundInfo {
        ensureChannel()
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(MAX_PROGRESS, progressPercent.coerceIn(0, MAX_PROGRESS), progressPercent <= 0)
            .build()
        return ForegroundInfo(
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationManager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "lamity_downloads"
        const val CHANNEL_NAME = "Downloads"
        const val MAX_PROGRESS = 100
    }
}
