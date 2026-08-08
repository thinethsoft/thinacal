package com.slanotifier.app

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface(private val context: Context) {

    private val prefs = context.getSharedPreferences("sla_app_settings", Context.MODE_PRIVATE)

    @JavascriptInterface
    fun getPersistentDeviceUuid(): String {
        var uuid = prefs.getString("device_uuid", null)
        if (uuid.isNullOrBlank()) {
            uuid = "dev_app_" + System.currentTimeMillis() + "_" + (1000..9999).random()
            prefs.edit().putString("device_uuid", uuid).apply()
        }
        return uuid!!
    }

    @JavascriptInterface
    fun getPersistentFcmToken(): String {
        var token = prefs.getString("fcm_token", null)
        if (token.isNullOrBlank()) {
            token = "cap_fcm_app_" + System.currentTimeMillis() + "_" + (1000..9999).random()
            prefs.edit().putString("fcm_token", token).apply()
        }
        return token!!
    }

    @JavascriptInterface
    fun triggerAlert(taskId: String, title: String, message: String) {
        val safeId = if (taskId.isBlank()) "task_" + System.currentTimeMillis() else taskId
        val safeTitle = if (title.isBlank()) "New Task Alert" else title
        val safeMsg = if (message.isBlank()) "Attention required for SLA task" else message

        TaskManager.addUnacknowledgedTask(context, safeId, safeTitle, safeMsg)
        NotificationHelper.triggerSlaNotification(context, safeId, safeTitle, safeMsg)
    }

    @JavascriptInterface
    fun acknowledgeTask(taskId: String) {
        if (taskId.isNotBlank()) {
            TaskManager.acknowledgeTask(context, taskId)
        }
    }

    @JavascriptInterface
    fun acknowledgeAllTasks() {
        TaskManager.acknowledgeAllTasks(context)
    }

    @JavascriptInterface
    fun getDeviceToken(): String {
        return getPersistentFcmToken()
    }

    @JavascriptInterface
    fun openUrlSettings() {
        if (context is MainActivity) {
            context.runOnUiThread {
                val intent = android.content.Intent(context, UrlSettingsActivity::class.java)
                context.startActivity(intent)
            }
        }
    }

    @JavascriptInterface
    fun showToast(toast: String) {
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
    }
}
