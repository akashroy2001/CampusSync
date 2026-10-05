package com.example.data.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.DaySchedule
import com.example.data.model.ScheduleClass
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "campus_sync_classes"
    private const val CHANNEL_NAME = "Class Reminders & Schedule Alerts"

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders 10 minutes prior to university class start times"
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        content: String
    ) {
        initChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(notificationId, builder.build())
    }

    fun scheduleClassReminder(
        context: Context,
        scheduleClass: ScheduleClass,
        leadMinutes: Int = 10
    ) {
        if (scheduleClass.isHoliday || scheduleClass.isFreePeriod) return

        val dtf1 = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val classDate = try {
            LocalDate.parse(scheduleClass.dateStr, dtf1)
        } catch (e: Exception) {
            try {
                LocalDate.parse(scheduleClass.dateStr, dtf2)
            } catch (e2: Exception) {
                LocalDate.now()
            }
        }

        val classStartTime = LocalTime.of(scheduleClass.slot.startHour, scheduleClass.slot.startMinute)
        val reminderTime = LocalDateTime.of(classDate, classStartTime).minusMinutes(leadMinutes.toLong())

        val reminderEpochMillis = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (reminderEpochMillis <= System.currentTimeMillis()) {
            return // Past time
        }

        val intent = Intent(context, ClassReminderReceiver::class.java).apply {
            action = ClassReminderReceiver.ACTION_CLASS_REMINDER
            putExtra(ClassReminderReceiver.EXTRA_COURSE_NAME, scheduleClass.displayTitle)
            putExtra(ClassReminderReceiver.EXTRA_SLOT_TIME, scheduleClass.slot.timeRange)
            putExtra(ClassReminderReceiver.EXTRA_FACULTY, scheduleClass.facultyName)
        }

        val reqCode = (scheduleClass.dateStr + scheduleClass.slot.slotNumber).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        try {
            alarmManager?.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderEpochMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled reminder for ${scheduleClass.displayTitle} at $reminderTime")
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not schedule exact alarm: ${e.message}")
        }
    }

    fun scheduleTodayReminders(context: Context, daySchedule: DaySchedule?) {
        if (daySchedule == null || daySchedule.isHoliday) return
        for (item in daySchedule.classes) {
            scheduleClassReminder(context, item, 10)
        }
    }

    fun sendTestNotification(context: Context) {
        showNotification(
            context = context,
            notificationId = 1001,
            title = "Class Starting in 10 mins",
            content = "Marketing Management I - 15 • 09:15-10:45 with Prof. Sumit Saxena"
        )
    }
}
