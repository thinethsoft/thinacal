package com.slanotifier.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

data class SlaTask(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

object TaskManager {
    private const val PREFS_NAME = "sla_tasks_prefs"
    private const val KEY_UNACKNOWLEDGED_TASKS = "unacknowledged_tasks"
    private const val REQUEST_CODE_ALARM = 888

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun addUnacknowledgedTask(context: Context, id: String, title: String, message: String) {
        val tasks = getUnacknowledgedTasks(context).toMutableList()
        // Avoid duplicate task ids
        tasks.removeAll { it.id == id }
        tasks.add(SlaTask(id, title, message))

        saveTasks(context, tasks)
        Log.d("TaskManager", "Added task $id. Total unacknowledged: ${tasks.size}")

        // Schedule repeating alert alarm every 1 minute
        scheduleRepeatingAlarm(context)
    }

    @Synchronized
    fun acknowledgeTask(context: Context, id: String) {
        val tasks = getUnacknowledgedTasks(context).toMutableList()
        val removed = tasks.removeAll { it.id == id }
        if (removed) {
            saveTasks(context, tasks)
            Log.d("TaskManager", "Acknowledged task $id. Remaining: ${tasks.size}")
        }
        if (tasks.isEmpty()) {
            cancelRepeatingAlarm(context)
        }
    }

    @Synchronized
    fun acknowledgeAllTasks(context: Context) {
        saveTasks(context, emptyList())
        cancelRepeatingAlarm(context)
        Log.d("TaskManager", "Acknowledged all tasks.")
    }

    @Synchronized
    fun getUnacknowledgedTasks(context: Context): List<SlaTask> {
        val jsonStr = getPrefs(context).getString(KEY_UNACKNOWLEDGED_TASKS, null) ?: return emptyList()
        val list = mutableListOf<SlaTask>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SlaTask(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        message = obj.getString("message"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("TaskManager", "Error parsing tasks JSON", e)
        }
        return list
    }

    private fun saveTasks(context: Context, tasks: List<SlaTask>) {
        val array = JSONArray()
        for (task in tasks) {
            val obj = JSONObject()
            obj.put("id", task.id)
            obj.put("title", task.title)
            obj.put("message", task.message)
            obj.put("timestamp", task.timestamp)
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_UNACKNOWLEDGED_TASKS, array.toString()).apply()
    }

    fun scheduleRepeatingAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REPEATING_ALERT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Repeat alarm every 60 seconds (1 minute) for SLA urgency
        val intervalMillis = 60 * 1000L
        val triggerAt = SystemClock.elapsedRealtime() + intervalMillis

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                intervalMillis,
                pendingIntent
            )
            Log.d("TaskManager", "Scheduled repeating SLA alert alarm every 60s")
        } catch (e: Exception) {
            Log.e("TaskManager", "Failed to schedule alarm", e)
        }
    }

    fun cancelRepeatingAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REPEATING_ALERT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("TaskManager", "Cancelled repeating SLA alert alarm.")
        }
    }
}
