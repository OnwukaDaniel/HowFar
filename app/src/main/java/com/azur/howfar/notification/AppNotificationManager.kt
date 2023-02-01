package com.azur.howfar.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.azur.howfar.R

class AppNotificationManager(private val context: Context) {
    /**
     * Create a notification channel
     * @param channelId The id of the channel. Must be unique per package. The value may be truncated if it is too long.
     * @param channelName The user visible name of the channel.
     * @param notificationImportance NotificationImportance, default is NotificationManager.IMPORTANCE_LOW
     * @param desc the user visible description of this channel.
     *
     * */
    fun createChannel(
        channelId: String,
        channelName: String,
        notificationImportance: Int = NotificationManager.IMPORTANCE_LOW,
        desc: String
    ) {
        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, channelName, notificationImportance).apply { description = desc }
        channel.enableLights(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
    }

    fun createNormalNotification(
        content: String,
        channelName: String,
        title: String,
        activityPendingIntent: PendingIntent,
        autoCancel: Boolean = true,
        notificationIndex: Int,
        priority: Int = NotificationCompat.PRIORITY_HIGH,
    ) {
        val notification = when {
            content != "" -> NotificationCompat.Builder(context, channelName)
                .setSmallIcon(R.drawable.app_icon_sec)
                .setColor(Color.BLUE)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(activityPendingIntent)
                .setAutoCancel(autoCancel)
                .setPriority(priority)
            else -> NotificationCompat.Builder(context, channelName)
                .setSmallIcon(R.drawable.app_icon_sec)
                .setColor(Color.BLUE)
                .setContentTitle(title)
                .setContentIntent(activityPendingIntent)
                .setAutoCancel(autoCancel)
                .setPriority(priority)
        }
        with(NotificationManagerCompat.from(context)) {
            notify(notificationIndex, notification.build())
        }
    }
}