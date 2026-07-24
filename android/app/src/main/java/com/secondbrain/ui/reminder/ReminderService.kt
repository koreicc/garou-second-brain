package com.secondbrain.ui.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Notification channel constants for task reminders.
 */
object ReminderChannel {
    const val CHANNEL_ID = "task_reminders"
    const val CHANNEL_NAME = "Task Reminders"
    const val CHANNEL_DESC = "Reminders for upcoming and due tasks"
}

/**
 * Creates the notification channel for task reminders.
 * Must be called once in Application.onCreate() or MainActivity.onCreate().
 */
fun createReminderNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            ReminderChannel.CHANNEL_ID,
            ReminderChannel.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = ReminderChannel.CHANNEL_DESC
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

/**
 * Shows a notification for a task due today or upcoming.
 */
fun showTaskReminderNotification(
    context: Context,
    taskId: String,
    taskTitle: String,
    taskDate: String
) {
    val intent = context.packageManager.getLaunchIntentForActivity(
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.component ?: return
    ) ?: return

    intent.putExtra("navigate_to_task", taskId)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

    val pendingIntent = PendingIntent.getActivity(
        context,
        taskId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, ReminderChannel.CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Task Due: $taskTitle")
        .setContentText("Due date: $taskDate")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    try {
        NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
    } catch (e: SecurityException) {
        // Notification permission not granted on Android 13+
    }
}

/**
 * Broadcast receiver that receives task reminder alarms.
 */
class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("task_id") ?: return
        val taskTitle = intent.getStringExtra("task_title") ?: return
        val taskDate = intent.getStringExtra("task_date") ?: return
        showTaskReminderNotification(context, taskId, taskTitle, taskDate)
    }
}

/**
 * Schedules a reminder alarm for a task.
 */
fun scheduleTaskReminder(
    context: Context,
    taskId: String,
    taskTitle: String,
    taskDate: String,
    triggerAtMillis: Long
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    val intent = Intent(context, TaskReminderReceiver::class.java).apply {
        putExtra("task_id", taskId)
        putExtra("task_title", taskTitle)
        putExtra("task_date", taskDate)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        taskId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
}

/**
 * Cancels a scheduled reminder alarm.
 */
fun cancelTaskReminder(context: Context, taskId: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    val intent = Intent(context, TaskReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        taskId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.cancel(pendingIntent)
}