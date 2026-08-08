package com.slanotifier.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_ID = "sla_high_priority_channel"
    private const val CHANNEL_NAME = "Critical SLA Task Alerts"
    private const val ALARM_CHANNEL_ID = "sla_continuous_alarm_channel"
    private const val ALARM_CHANNEL_NAME = "Incoming Call Alarm Alerts"
    private const val NOTIFICATION_ID_BASE = 9000

    private var activeRingtone: Ringtone? = null

    @Synchronized
    fun startContinuousRingtone(context: Context) {
        try {
            stopContinuousRingtone()
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            activeRingtone = RingtoneManager.getRingtone(context, alarmUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                activeRingtone?.isLooping = true
            }
            activeRingtone?.play()
            Log.d("NotificationHelper", "Started continuous ringing call alarm")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error playing ringtone", e)
        }
    }

    @Synchronized
    fun stopContinuousRingtone() {
        try {
            activeRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                    Log.d("NotificationHelper", "Stopped continuous ringing call alarm")
                }
            }
            activeRingtone = null
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error stopping ringtone", e)
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High Priority Channel for SLA Task Notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                enableLights(true)
            }

            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                ALARM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Continuous Ringing Call Alarm Channel for Emergency SLA Tasks"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                enableLights(true)
            }

            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(alarmChannel)
            Log.d("NotificationHelper", "Created SLA Notification Channels")
        }
    }

    fun triggerSlaNotification(
        context: Context,
        taskId: String,
        title: String,
        message: String,
        isAlarm: Boolean = false,
        targetUrl: String = ""
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(context)

        // Wake screen if device is locked/asleep
        wakeUpScreen(context)

        if (isAlarm) {
            startContinuousRingtone(context)
        }

        val targetChannel = if (isAlarm) ALARM_CHANNEL_ID else CHANNEL_ID

        // Intent when notification is clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TASK_ID", taskId)
            if (!targetUrl.isBlank()) {
                putExtra("TARGET_URL", targetUrl)
            }
            putExtra("EXTRA_ACTION", if (isAlarm) "ANSWER_CALL" else "ACKNOWLEDGE")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to stop continuous alarm / reject call
        val stopAlarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.slanotifier.app.ACTION_STOP_ALARM"
        }
        val stopAlarmPendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + "_stop").hashCode(),
            stopAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, targetChannel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setVibrate(if (isAlarm) longArrayOf(0, 1000, 500, 1000) else longArrayOf(0, 300, 150, 300))
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)

        if (!isAlarm) {
            builder.setSound(soundUri)
        }

        if (isAlarm) {
            val answerIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (!targetUrl.isBlank()) {
                    putExtra("TARGET_URL", targetUrl)
                }
                putExtra("EXTRA_ACTION", "ANSWER_CALL")
            }
            val answerPendingIntent = PendingIntent.getActivity(
                context,
                (taskId + "_answer").hashCode(),
                answerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(
                android.R.drawable.ic_menu_call,
                "📞 ANSWER CALL",
                answerPendingIntent
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "🛑 REJECT",
                stopAlarmPendingIntent
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_menu_view,
                "👁️ VIEW TASK",
                pendingIntent
            )
        }

        val notifId = NOTIFICATION_ID_BASE + (Math.abs(taskId.hashCode()) % 1000)
        notificationManager.notify(notifId, builder.build())
        Log.d("NotificationHelper", "Triggered SLA Notification ID $notifId for task $taskId (isAlarm: $isAlarm)")
    }

    private fun wakeUpScreen(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "SlaTaskNotifier:WakeLock"
            )
            wakeLock.acquire(5000) // Keep screen awake for 5s
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error acquiring WakeLock", e)
        }
    }
}
