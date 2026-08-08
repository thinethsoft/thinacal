package com.slanotifier.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REPEATING_ALERT = "com.slanotifier.app.ACTION_REPEATING_ALERT"
        const val ACTION_STOP_ALARM = "com.slanotifier.app.ACTION_STOP_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AlarmReceiver", "Received action: $action")

        if (action == ACTION_STOP_ALARM) {
            NotificationHelper.stopContinuousRingtone()
            Log.d("AlarmReceiver", "Action STOP_ALARM executed. Continuous ringtone stopped.")
            return
        }

        val tasks = TaskManager.getUnacknowledgedTasks(context)
        if (tasks.isNotEmpty()) {
            Log.d("AlarmReceiver", "Unacknowledged tasks count: ${tasks.size}. Re-ringing alert tone!")
            for (task in tasks) {
                NotificationHelper.triggerSlaNotification(
                    context = context,
                    taskId = task.id,
                    title = task.title,
                    message = "[REPEATING SLA ALERT] ${task.message}"
                )
            }
        } else {
            Log.d("AlarmReceiver", "No unacknowledged tasks. Cancelling alarm.")
            TaskManager.cancelRepeatingAlarm(context)
        }
    }
}
