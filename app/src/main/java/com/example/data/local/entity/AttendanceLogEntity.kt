package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity to log individual class attendance entries (e.g. marking Present or Absent)
 * linked to a CourseAttendanceEntity.
 */
@Entity(
    tableName = "attendance_logs",
    foreignKeys = [
        ForeignKey(
            entity = CourseAttendanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["courseId"]),
        Index(value = ["timestamp"])
    ]
)
data class AttendanceLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long,
    val courseName: String,
    val dateStr: String,
    val wasAttended: Boolean,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
