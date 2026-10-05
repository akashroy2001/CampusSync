package com.example.data.model

import kotlin.math.ceil
import kotlin.math.floor

const val REQUIRED_ATTENDANCE_THRESHOLD = 80.0f

enum class AttendanceZoneStatus {
    SAFE,        // >= 80% and safe buffer available
    WARNING,     // >= 80% but 0 bunks remaining (on the brink), or 75%..79.9%
    CRITICAL,    // < 80% shortage
    NOT_STARTED  // 0 classes conducted yet
}

/**
 * Detailed progress evaluation for a single course vs the mandatory 80% threshold.
 */
data class CourseAttendanceProgress(
    val courseId: Long = 0,
    val courseName: String,
    val facultyName: String = "",
    val attendedClasses: Int,
    val totalConductedClasses: Int,
    val totalTermSessions: Int = 20,
    val requiredThreshold: Float = REQUIRED_ATTENDANCE_THRESHOLD,
    val isLmsSynced: Boolean = true,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
    val isNotStarted: Boolean get() = totalConductedClasses == 0

    val currentPercentage: Float get() = if (totalConductedClasses > 0) {
        (attendedClasses.toFloat() / totalConductedClasses.toFloat()) * 100f
    } else {
        0f
    }

    val missedClasses: Int get() = (totalConductedClasses - attendedClasses).coerceAtLeast(0)

    val progressFraction: Float get() = (currentPercentage / 100f).coerceIn(0f, 1f)

    /**
     * Difference between current attendance and the 80% requirement.
     * Positive = surplus above safe zone.
     * Negative = deficit below safe zone.
     */
    val thresholdDelta: Float get() = currentPercentage - requiredThreshold

    val isInSafeZone: Boolean get() = currentPercentage >= requiredThreshold && totalConductedClasses > 0

    /**
     * How many future classes can be missed consecutively while maintaining >= 80% attendance.
     * Formula: attended / (conducted + bunks) >= 0.80  =>  bunks <= (attended / 0.80) - conducted
     */
    val safeBunksRemaining: Int get() {
        if (totalConductedClasses == 0) {
            return (totalTermSessions * (1.0f - requiredThreshold / 100f)).toInt()
        }
        val maxConducted = floor(attendedClasses / (requiredThreshold / 100.0)).toInt()
        return (maxConducted - totalConductedClasses).coerceAtLeast(0)
    }

    /**
     * How many upcoming consecutive classes must be attended to recover to >= 80%.
     * Formula: (attended + n) / (conducted + n) >= 0.80
     * 0.20 * n >= 0.80 * conducted - attended => n >= 4 * conducted - 5 * attended
     */
    val classesNeededToRecover: Int get() {
        if (totalConductedClasses == 0 || currentPercentage >= requiredThreshold) return 0
        val needed = (4 * totalConductedClasses - 5 * attendedClasses)
        return needed.coerceAtLeast(0)
    }

    /**
     * What the percentage will become if the student attends the next class.
     */
    val percentageIfNextAttended: Float get() {
        val nextConducted = totalConductedClasses + 1
        return ((attendedClasses + 1).toFloat() / nextConducted.toFloat()) * 100f
    }

    /**
     * What the percentage will become if the student misses the next class.
     */
    val percentageIfNextMissed: Float get() {
        val nextConducted = totalConductedClasses + 1
        return (attendedClasses.toFloat() / nextConducted.toFloat()) * 100f
    }

    val zoneStatus: AttendanceZoneStatus get() {
        return when {
            totalConductedClasses == 0 -> AttendanceZoneStatus.NOT_STARTED
            currentPercentage < requiredThreshold -> AttendanceZoneStatus.CRITICAL
            safeBunksRemaining == 0 -> AttendanceZoneStatus.WARNING
            else -> AttendanceZoneStatus.SAFE
        }
    }
}

/**
 * Aggregated attendance progress metrics across all enrolled courses.
 */
data class OverallAttendanceProgress(
    val totalAttended: Int,
    val totalConducted: Int,
    val requiredThreshold: Float = REQUIRED_ATTENDANCE_THRESHOLD,
    val safeCoursesCount: Int = 0,
    val warningCoursesCount: Int = 0,
    val criticalCoursesCount: Int = 0,
    val totalCoursesCount: Int = 0
) {
    val overallPercentage: Float get() = if (totalConducted > 0) {
        (totalAttended.toFloat() / totalConducted.toFloat()) * 100f
    } else {
        100f
    }

    val totalMissed: Int get() = (totalConducted - totalAttended).coerceAtLeast(0)

    val thresholdDelta: Float get() = overallPercentage - requiredThreshold

    val isInSafeZone: Boolean get() = overallPercentage >= requiredThreshold

    val overallSafeBunks: Int get() {
        if (totalConducted == 0) return 0
        val maxConducted = floor(totalAttended / (requiredThreshold / 100.0)).toInt()
        return (maxConducted - totalConducted).coerceAtLeast(0)
    }

    val overallClassesToRecover: Int get() {
        if (totalConducted == 0 || overallPercentage >= requiredThreshold) return 0
        val needed = (4 * totalConducted - 5 * totalAttended)
        return needed.coerceAtLeast(0)
    }

    val zoneStatus: AttendanceZoneStatus get() {
        return when {
            totalConducted == 0 -> AttendanceZoneStatus.NOT_STARTED
            overallPercentage < requiredThreshold -> AttendanceZoneStatus.CRITICAL
            overallSafeBunks == 0 || warningCoursesCount > 0 -> AttendanceZoneStatus.WARNING
            else -> AttendanceZoneStatus.SAFE
        }
    }
}
