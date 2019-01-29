package `in`.bitotsav.notification.utils

import `in`.bitotsav.R
import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import java.util.*

enum class Channel(val id: String, val channelName: String){
    ANNOUNCEMENT("announcement", "Announcement"),
    EVENT("event", "Event"),
    PM("pm", "Private Message"),
    RESULT("result", "Result"),
    STARRED("starred", "Starred")
}

/**
 * Create and show a notification
 */
fun displayNotification(
    title: String,
    content: String,
    timestamp: Long,
    channel: Channel,
    intent: Intent,
    context: Context
) {
    val uniqueId = (Math.log(Date().time.toDouble()) * 1000000000000000L % Integer.MAX_VALUE).toInt()

    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

    val pendingIntent = PendingIntent.getActivity(
        context,
        uniqueId,
        intent,
        PendingIntent.FLAG_ONE_SHOT
    )

    val bigTextStyle = NotificationCompat.BigTextStyle()
    bigTextStyle.bigText(content)

//    TODO("Set small icon, large icon and color")
    val channelId = channel.id
    val notificationBuilder = NotificationCompat.Builder(context, channelId)
//        TODO: https://stackoverflow.com/questions/25317659/how-to-fix-android-app-remoteserviceexception-bad-notification-posted-from-pac
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setStyle(bigTextStyle)
        .setContentTitle(title)
        .setContentText(content)
        .setDefaults(Notification.DEFAULT_ALL)
        .setWhen(timestamp)
        .setShowWhen(true)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(uniqueId, notificationBuilder.build())
}

@TargetApi(26)
fun createNotificationChannels(context: Context){
//    TODO("Fix notification channel for MIUI")
    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    Channel.values().forEach {
        val channel = NotificationChannel(it.id, it.channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
        setSound(defaultSoundUri, null)
        enableLights(true)
        enableVibration(true)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    }
    notificationManager.createNotificationChannel(channel)
    }
}