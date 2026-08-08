package com.slanotifier.app

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_SERVICE", "Refreshed FCM Token: $token")
        val prefs = getSharedPreferences("sla_app_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_SERVICE", "From: ${remoteMessage.from}")

        var title = remoteMessage.notification?.title
        var body = remoteMessage.notification?.body

        if (title.isNullOrBlank()) {
            title = remoteMessage.data["title"] ?: "E Shop Alert"
        }
        if (body.isNullOrBlank()) {
            body = remoteMessage.data["body"] ?: remoteMessage.data["message"] ?: "New SLA Task Notification"
        }

        val level = remoteMessage.data["level"] ?: remoteMessage.data["action"] ?: ""

        if (level == "stop_alarm") {
            NotificationHelper.stopContinuousRingtone()
            Log.d("FCM_SERVICE", "Remote signal: STOP ALARM received")
            return
        }

        val tag = remoteMessage.data["tag"] ?: ("fcm_" + System.currentTimeMillis())
        val isAlarm = (level == "alarm" || title!!.contains("ALARM", ignoreCase = true) || title.contains("CALL", ignoreCase = true))

        // Trigger native notification with sound and vibration even when app is OPEN in foreground
        NotificationHelper.triggerSlaNotification(this, tag, title!!, body!!, isAlarm = isAlarm)
    }
}
