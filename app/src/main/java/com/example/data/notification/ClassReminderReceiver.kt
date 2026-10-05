package com.example.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ClassReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: "Upcoming Class"
        val slotTime = intent.getStringExtra(EXTRA_SLOT_TIME) ?: ""
        val faculty = intent.getStringExtra(EXTRA_FACULTY) ?: ""

        val title = "Class Starting in 10 mins"
        val content = if (faculty.isNotBlank()) {
            "$courseName • $slotTime with $faculty"
        } else {
            "$courseName • $slotTime"
        }

        NotificationHelper.showNotification(
            context = context,
            notificationId = (courseName + slotTime).hashCode(),
            title = title,
            content = content
        )
    }

    companion object {
        const val ACTION_CLASS_REMINDER = "com.aistudio.campussync.ACTION_CLASS_REMINDER"
        const val EXTRA_COURSE_NAME = "extra_course_name"
        const val EXTRA_SLOT_TIME = "extra_slot_time"
        const val EXTRA_FACULTY = "extra_faculty"
    }
}
