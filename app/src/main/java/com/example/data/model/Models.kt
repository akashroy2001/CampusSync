package com.example.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class Section(val code: String, val title: String, val description: String) {
    SECTION_A("Section A", "Section A", "MBA 2026-28 • Term II (Section A)"),
    SECTION_B("Section B", "Section B", "MBA 2026-28 • Term II (Section B)"),
    SECTION_C("Section C", "Section C", "MBA 2026-28 • Term II (Section C)"),
    SECTION_D("Section D", "Section D", "MBA 2026-28 • Term II (Section D)");

    val shortPill: String get() = when (this) {
        SECTION_A -> "Sec A"
        SECTION_B -> "Sec B"
        SECTION_C -> "Sec C"
        SECTION_D -> "Sec D"
    }

    companion object {
        fun fromString(value: String?): Section {
            if (value == null) return SECTION_A
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                it.code.equals(value, ignoreCase = true) ||
                it.title.equals(value, ignoreCase = true) ||
                it.shortPill.equals(value, ignoreCase = true)
            } ?: SECTION_A
        }
    }
}

data class ClassSlot(
    val slotNumber: Int,
    val timeRange: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    val startMinutes: Int get() = startHour * 60 + startMinute
    val endMinutes: Int get() = endHour * 60 + endMinute

    val formattedStartTime: String get() = String.format("%02d:%02d", startHour, startMinute)
    val formattedEndTime: String get() = String.format("%02d:%02d", endHour, endMinute)

    companion object {
        val ALL_SLOTS = listOf(
            ClassSlot(1, "09:15-10:45", 9, 15, 10, 45),
            ClassSlot(2, "11:00-12:30", 11, 0, 12, 30),
            ClassSlot(3, "13:30-15:00", 13, 30, 15, 0),
            ClassSlot(4, "15:15-16:45", 15, 15, 16, 45),
            ClassSlot(5, "17:00-18:30", 17, 0, 18, 30),
            ClassSlot(6, "18:45-20:15", 18, 45, 20, 15)
        )

        fun findSlotByRange(range: String): ClassSlot? {
            val normalized = range.replace(" ", "").replace("–", "-")
            return ALL_SLOTS.firstOrNull { it.timeRange == normalized }
        }
    }
}

enum class SlotStatus {
    LIVE_NOW,
    UP_NEXT,
    UPCOMING,
    COMPLETED,
    HOLIDAY,
    FREE_PERIOD
}

data class ScheduleClass(
    val dateStr: String, // "03-09-2026"
    val dayOfWeek: String, // "Thursday"
    val slot: ClassSlot,
    val courseName: String,
    val sessionNumber: String,
    val facultyName: String,
    val isHoliday: Boolean = false,
    val holidayName: String = "",
    val status: SlotStatus = SlotStatus.UPCOMING,
    val minutesUntilStart: Int = 0,
    val minutesUntilEnd: Int = 0
) {
    val isFreePeriod: Boolean get() = !isHoliday && courseName.isBlank()
    val displayTitle: String get() = when {
        isHoliday -> holidayName
        isFreePeriod -> "Free Period / Self Study"
        sessionNumber.isNotBlank() -> "$courseName ($sessionNumber)"
        else -> courseName
    }
}

data class DaySchedule(
    val dateStr: String, // "03-09-2026"
    val dayName: String, // "Thursday"
    val isHoliday: Boolean,
    val holidayTitle: String,
    val classes: List<ScheduleClass>
)

enum class MealType(
    val displayName: String,
    val timeWindow: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    BREAKFAST("Breakfast", "8:00 AM – 10:00 AM", 8, 0, 10, 0),
    LUNCH("Lunch", "12:00 PM – 2:15 PM", 12, 0, 14, 15),
    SNACKS("Snacks", "5:00 PM – 6:30 PM", 17, 0, 18, 30),
    DINNER("Dinner", "7:45 PM – 10:00 PM", 19, 45, 22, 0);

    val startMinutes: Int get() = startHour * 60 + startMinute
    val endMinutes: Int get() = endHour * 60 + endMinute
}

enum class MealStatus {
    SERVING_NOW,
    UP_NEXT,
    UPCOMING,
    ENDED
}

data class MessDish(
    val name: String,
    val isNonVeg: Boolean
)

data class MealMenu(
    val mealType: MealType,
    val dishes: List<MessDish>,
    val specialNote: String = "",
    val status: MealStatus = MealStatus.UPCOMING,
    val timeRemainingText: String = ""
)

data class DayMessMenu(
    val dayName: String, // "Monday" .. "Sunday"
    val dayIndex: Int, // 0..6
    val meals: List<MealMenu>
)

const val MANDATORY_ATTENDANCE_THRESHOLD = 80.0f

data class CourseAttendance(
    val courseName: String,
    val facultyName: String = "",
    val attendedClasses: Int,
    val totalConductedClasses: Int,
    val totalTermSessions: Int = 20,
    val isLmsSynced: Boolean = true
) {
    val isNotStarted: Boolean
        get() = totalConductedClasses == 0

    val percentage: Float
        get() = if (totalConductedClasses > 0) {
            (attendedClasses.toFloat() / totalConductedClasses.toFloat()) * 100f
        } else {
            0f
        }

    val missedClasses: Int
        get() = (totalConductedClasses - attendedClasses).coerceAtLeast(0)

    val safeBunksRemaining: Int
        get() {
            if (totalConductedClasses == 0) return 0
            val maxConductedForAttended = (attendedClasses / 0.80).toInt()
            return (maxConductedForAttended - totalConductedClasses).coerceAtLeast(0)
        }

    val classesNeededFor80: Int
        get() {
            if (percentage >= 80f || totalConductedClasses == 0) return 0
            val needed = (4 * totalConductedClasses - 5 * attendedClasses)
            return needed.coerceAtLeast(0)
        }

    val isBelowThreshold: Boolean
        get() = totalConductedClasses > 0 && percentage < 80.0f
}

data class OverallAttendance(
    val totalAttended: Int,
    val totalConducted: Int,
    val courses: List<CourseAttendance>,
    val lastSyncTimestampMillis: Long = System.currentTimeMillis(),
    val isLmsConnected: Boolean = true,
    val syncStatusText: String = "Term II Attendance Synced"
) {
    val overallPercentage: Float
        get() = if (totalConducted > 0) {
            (totalAttended.toFloat() / totalConducted.toFloat()) * 100f
        } else {
            100f
        }

    val totalMissed: Int
        get() = (totalConducted - totalAttended).coerceAtLeast(0)

    val safeBunksRemaining: Int
        get() {
            if (totalConducted == 0) return 0
            val maxConducted = (totalAttended / 0.80).toInt()
            return (maxConducted - totalConducted).coerceAtLeast(0)
        }

    val classesNeededFor80: Int
        get() {
            if (overallPercentage >= 80f || totalConducted == 0) return 0
            val needed = (4 * totalConducted - 5 * totalAttended)
            return needed.coerceAtLeast(0)
        }

    val isBelowThreshold: Boolean
        get() = totalConducted > 0 && overallPercentage < 80.0f
}

