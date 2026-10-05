package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.CourseAttendance
import com.example.data.model.MANDATORY_ATTENDANCE_THRESHOLD

/**
 * Room Database Entity for tracking attendance for an academic course.
 * Includes tracking fields for attended classes, conducted classes, total sessions,
 * and the required safe zone threshold (80% and above).
 */
@Entity(
    tableName = "course_attendance",
    indices = [Index(value = ["courseName"], unique = true)]
)
data class CourseAttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseName: String,
    val facultyName: String = "",
    val attendedClasses: Int = 0,
    val totalConductedClasses: Int = 0,
    val totalTermSessions: Int = 20,
    val requiredThresholdPercentage: Float = MANDATORY_ATTENDANCE_THRESHOLD,
    val isLmsSynced: Boolean = true,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Converts the entity to domain CourseAttendance model
     */
    fun toDomain(): CourseAttendance {
        return CourseAttendance(
            courseName = courseName,
            facultyName = facultyName,
            attendedClasses = attendedClasses,
            totalConductedClasses = totalConductedClasses,
            totalTermSessions = totalTermSessions,
            isLmsSynced = isLmsSynced
        )
    }

    companion object {
        fun fromDomain(domain: CourseAttendance, id: Long = 0): CourseAttendanceEntity {
            return CourseAttendanceEntity(
                id = id,
                courseName = domain.courseName,
                facultyName = domain.facultyName,
                attendedClasses = domain.attendedClasses,
                totalConductedClasses = domain.totalConductedClasses,
                totalTermSessions = domain.totalTermSessions,
                requiredThresholdPercentage = MANDATORY_ATTENDANCE_THRESHOLD,
                isLmsSynced = domain.isLmsSynced,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
        }
    }
}
