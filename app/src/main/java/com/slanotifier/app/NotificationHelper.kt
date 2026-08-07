package com.slanotifier.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object NotificationHelper {
    private const val CHANNEL_ID = "sla_high_priority_channel"
    private const val CHANNEL_NAME = "Critical SLA Task Alerts"
    private const val NOTIFICATION_ID_BASE = 9000

    fun getTinginSoundUri(context: Context): Uri {
        // Try raw resource first
        val rawResId = context.resources.getIdentifier("tingin", "raw", context.packageName)
        if (rawResId != 0) {
            return Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/tingin")
        }

        // Fallback: Generate tingin.wav file in app storage dynamically
        val soundFile = File(context.filesDir, "tingin_tone.wav")
        if (!soundFile.exists()) {
            generateTinginWavFile(soundFile)
        }

        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", soundFile)
        } catch (e: Exception) {
            Uri.fromFile(soundFile)
        }
    }

    private fun generateTinginWavFile(outputFile: File) {
        try {
            val sampleRate = 44100
            val duration1 = 0.15 // 150ms for "Ting" (1760 Hz)
            val duration2 = 0.35 // 350ms for "In" (2637 Hz)

            val numSamples1 = (sampleRate * duration1).toInt()
            val numSamples2 = (sampleRate * duration2).toInt()
            val totalSamples = numSamples1 + numSamples2

            val pcmData = ByteArray(totalSamples * 2)
            val buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)

            // Tone 1: 1760 Hz
            for (i in 0 until numSamples1) {
                val t = i.toDouble() / sampleRate
                val decay = Math.exp(-i.toDouble() / (sampleRate * 0.05))
                val sample = Math.sin(2.0 * Math.PI * 1760.0 * t) * decay * 0.8
                val val16 = (Math.max(-1.0, Math.min(1.0, sample)) * 32767).toInt().toShort()
                buffer.putShort(val16)
            }

            // Tone 2: 2637 Hz
            for (i in 0 until numSamples2) {
                val t = i.toDouble() / sampleRate
                val decay = Math.exp(-i.toDouble() / (sampleRate * 0.12))
                val sample = Math.sin(2.0 * Math.PI * 2637.0 * t) * decay * 0.9
                val val16 = (Math.max(-1.0, Math.min(1.0, sample)) * 32767).toInt().toShort()
                buffer.putShort(val16)
            }

            val dataSize = pcmData.size
            val chunkSize = 36 + dataSize
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

            header.put("RIFF".toByteArray())
            header.putInt(chunkSize)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16) // Subchunk1Size
            header.putShort(1.toShort()) // PCM
            header.putShort(1.toShort()) // Mono
            header.putInt(sampleRate)
            header.putInt(sampleRate * 2) // ByteRate
            header.putShort(2.toShort()) // BlockAlign
            header.putShort(16.toShort()) // BitsPerSample
            header.put("data".toByteArray())
            header.putInt(dataSize)

            FileOutputStream(outputFile).use { fos ->
                fos.write(header.array())
                fos.write(pcmData)
            }
            Log.d("NotificationHelper", "Generated Tingin WAV tone at ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error generating WAV file", e)
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
                description = "High Priority Channel for SLA Task Notifications with Tingin tone"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                enableLights(true)
            }

            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationHelper", "Created SLA Notification Channel with sound $soundUri")
        }
    }

    fun triggerSlaNotification(context: Context, taskId: String, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(context)

        // Wake screen if device is locked/asleep
        wakeUpScreen(context)

        // Intent when notification is clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_ACTION", "ACKNOWLEDGE")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Fullscreen intent for instant wakeup / heads-up alert
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            (taskId + "_fs").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(
                android.R.drawable.ic_menu_view,
                "View Task & Acknowledge",
                pendingIntent
            )

        val notifId = NOTIFICATION_ID_BASE + (Math.abs(taskId.hashCode()) % 1000)
        notificationManager.notify(notifId, builder.build())
        Log.d("NotificationHelper", "Triggered SLA Notification ID $notifId for task $taskId")
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
