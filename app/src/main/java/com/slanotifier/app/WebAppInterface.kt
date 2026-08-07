package com.slanotifier.app

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface(private val context: Context) {

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
        return "thinacal_device_" + (Math.abs(context.packageName.hashCode()) + System.currentTimeMillis() % 100000)
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
