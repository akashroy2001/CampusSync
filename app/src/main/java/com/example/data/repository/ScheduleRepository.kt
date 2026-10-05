package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.DaySchedule
import com.example.data.model.ScheduleClass
import com.example.data.model.SlotStatus
import com.example.data.parser.CsvParser
import com.example.data.parser.XlsxParser
import com.example.data.preferences.UserPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ScheduleRepository(private val context: Context) {

    private val prefs = UserPreferencesManager.getInstance(context)

    // Master list of day schedules for the cohort timetable
    private val _schedules = MutableStateFlow<List<DaySchedule>>(emptyList())
    val schedules: StateFlow<List<DaySchedule>> = _schedules.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val customScheduleFile: File
        get() = File(context.filesDir, "active_schedule_custom")

    suspend fun loadSchedule() = withContext(Dispatchers.IO) {
        _isLoading.value = true
        try {
            val list = if (customScheduleFile.exists()) {
                Log.d(TAG, "Loading schedule from custom storage file: ${customScheduleFile.absolutePath}")
                loadFromFile(customScheduleFile)
            } else {
                Log.d(TAG, "Loading schedule from default assets: $DEFAULT_CSV_ASSET")
                try {
                    context.assets.open(DEFAULT_CSV_ASSET).use { input ->
                        val rows = CsvParser.parseCsv(input)
                        CsvParser.convertCsvRowsToDaySchedules(rows)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "CSV asset load failed, trying revised CSV asset", e)
                    try {
                        context.assets.open(DEFAULT_REVISED_CSV_ASSET).use { input ->
                            val rows = CsvParser.parseCsv(input)
                            CsvParser.convertCsvRowsToDaySchedules(rows)
                        }
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed to load timetable from assets", e2)
                        emptyList()
                    }
                }
            }

            Log.d(TAG, "Loaded ${list.size} day schedules")
            _schedules.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading timetable schedule", e)
        } finally {
            _isLoading.value = false
        }
    }

    private fun loadFromFile(file: File): List<DaySchedule> {
        val fileName = file.name.lowercase()
        return FileInputStream(file).use { stream ->
            if (fileName.endsWith(".csv")) {
                val rows = CsvParser.parseCsv(stream)
                CsvParser.convertCsvRowsToDaySchedules(rows)
            } else {
                // Try XLSX
                val sheets = XlsxParser.parseXlsx(stream)
                val rows = sheets.values.firstOrNull() ?: emptyList()
                XlsxParser.convertRowsToDaySchedules(rows)
            }
        }
    }

    suspend fun saveCustomSchedule(input: InputStream, originalFileName: String = "schedule.csv"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val isCsv = originalFileName.lowercase().endsWith(".csv")
            val tempFile = File(context.cacheDir, "temp_schedule_${System.currentTimeMillis()}.${if (isCsv) "csv" else "xlsx"}")
            FileOutputStream(tempFile).use { out ->
                input.copyTo(out)
            }

            // Validate parse
            val parsedList = try {
                loadFromFile(tempFile)
            } catch (e: Exception) {
                tempFile.delete()
                return@withContext Result.failure(Exception("Could not parse schedule: ${e.message}"))
            }

            if (parsedList.isEmpty()) {
                tempFile.delete()
                return@withContext Result.failure(Exception("The uploaded file does not contain any readable timetable rows."))
            }

            // Overwrite active custom file
            if (customScheduleFile.exists()) {
                customScheduleFile.delete()
            }
            tempFile.copyTo(customScheduleFile, overwrite = true)
            tempFile.delete()

            prefs.markCustomScheduleUpdated(true)
            _schedules.value = parsedList

            Result.success("Timetable updated successfully with ${parsedList.size} schedule days!")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom schedule", e)
            Result.failure(e)
        }
    }

    suspend fun resetToDefault(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (customScheduleFile.exists()) {
                customScheduleFile.delete()
            }
            prefs.markCustomScheduleUpdated(false)
            loadSchedule()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset to default timetable", e)
            Result.failure(e)
        }
    }

    fun isCustomScheduleActive(): Boolean {
        return customScheduleFile.exists() && prefs.hasCustomSchedule()
    }

    /**
     * Finds and updates the day's schedule with real-time slot statuses
     */
    fun getUpdatedDaySchedule(
        targetDate: LocalDate,
        currentTime: LocalTime
    ): DaySchedule? {
        val allDays = _schedules.value
        if (allDays.isEmpty()) return null

        val dtf1 = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        // 1. First attempt exact date match
        val matched = allDays.firstOrNull { daySched ->
            val parsedDate = try {
                LocalDate.parse(daySched.dateStr, dtf1)
            } catch (e: Exception) {
                try {
                    LocalDate.parse(daySched.dateStr, dtf2)
                } catch (e2: Exception) {
                    null
                }
            }
            parsedDate == targetDate
        } ?: allDays.firstOrNull {
            // 2. Fallback to day of week name match if date string didn't parse
            it.dayName.equals(targetDate.dayOfWeek.name, ignoreCase = true)
        }

        if (matched == null) return null

        val today = LocalDate.now()
        val isFutureDay = targetDate.isAfter(today)
        val isPastDay = targetDate.isBefore(today)
        val isToday = targetDate.isEqual(today)

        val currentMinutes = currentTime.hour * 60 + currentTime.minute

        val updatedClasses = matched.classes.map { item ->
            if (item.isHoliday) {
                item.copy(status = SlotStatus.HOLIDAY)
            } else if (item.isFreePeriod) {
                item.copy(status = SlotStatus.FREE_PERIOD)
            } else {
                val start = item.slot.startMinutes
                val end = item.slot.endMinutes
                val status = when {
                    isFutureDay -> SlotStatus.UPCOMING
                    isPastDay -> SlotStatus.COMPLETED
                    // For today:
                    currentMinutes in start..end -> SlotStatus.LIVE_NOW
                    currentMinutes < start && (start - currentMinutes) <= 60 -> SlotStatus.UP_NEXT
                    currentMinutes < start -> SlotStatus.UPCOMING
                    else -> SlotStatus.COMPLETED
                }
                val minUntilStart = if (isToday) (start - currentMinutes).coerceAtLeast(0) else 0
                val minUntilEnd = if (isToday) (end - currentMinutes).coerceAtLeast(0) else 0
                item.copy(
                    status = status,
                    minutesUntilStart = minUntilStart,
                    minutesUntilEnd = minUntilEnd
                )
            }
        }

        return matched.copy(classes = updatedClasses)
    }

    /**
     * Active Class Info for Now Dashboard & Status widgets
     */
    data class ActiveClassInfo(
        val activeClass: ScheduleClass?,
        val nextClass: ScheduleClass?,
        val tomorrowUpcomingClass: ScheduleClass? = null,
        val tomorrowDateText: String? = null,
        val daySchedule: DaySchedule?,
        val isClassLiveNow: Boolean,
        val isAllClassesForTodayCompleted: Boolean = false,
        val statusText: String,
        val minutesRemaining: Int
    )

    fun getActiveClassInfo(
        targetDate: LocalDate = LocalDate.now(),
        currentTime: LocalTime = LocalTime.now()
    ): ActiveClassInfo {
        val daySchedule = getUpdatedDaySchedule(targetDate, currentTime)

        // Helper to find next upcoming lecture from tomorrow onwards (up to 7 days ahead)
        fun findTomorrowOrNextLecture(): Pair<ScheduleClass?, String?> {
            for (offset in 1..7) {
                val futureDate = targetDate.plusDays(offset.toLong())
                val futureSchedule = getUpdatedDaySchedule(futureDate, LocalTime.MIN)
                if (futureSchedule != null && !futureSchedule.isHoliday) {
                    val candidate = futureSchedule.classes.firstOrNull { !it.isFreePeriod && !it.isHoliday }
                    if (candidate != null) {
                        val label = if (offset == 1) {
                            "Tomorrow (${futureSchedule.dayName}, ${futureSchedule.dateStr})"
                        } else {
                            "${futureSchedule.dayName} (${futureSchedule.dateStr})"
                        }
                        return Pair(candidate, label)
                    }
                }
            }
            return Pair(null, null)
        }

        if (daySchedule == null) {
            val (tomClass, tomLabel) = findTomorrowOrNextLecture()
            return ActiveClassInfo(
                activeClass = null,
                nextClass = null,
                tomorrowUpcomingClass = tomClass,
                tomorrowDateText = tomLabel,
                daySchedule = null,
                isClassLiveNow = false,
                isAllClassesForTodayCompleted = true,
                statusText = if (tomClass != null) "No timetable today • Next: $tomLabel" else "No timetable available",
                minutesRemaining = 0
            )
        }

        val (tomorrowClass, tomorrowDateText) = findTomorrowOrNextLecture()

        if (daySchedule.isHoliday) {
            val holTitle = if (daySchedule.holidayTitle.isNotBlank()) daySchedule.holidayTitle else "Holiday • No classes today"
            return ActiveClassInfo(
                activeClass = null,
                nextClass = null,
                tomorrowUpcomingClass = tomorrowClass,
                tomorrowDateText = tomorrowDateText,
                daySchedule = daySchedule,
                isClassLiveNow = false,
                isAllClassesForTodayCompleted = true,
                statusText = holTitle,
                minutesRemaining = 0
            )
        }

        val nonFreeClasses = daySchedule.classes.filter { !it.isFreePeriod && !it.isHoliday }
        if (nonFreeClasses.isEmpty()) {
            return ActiveClassInfo(
                activeClass = null,
                nextClass = null,
                tomorrowUpcomingClass = tomorrowClass,
                tomorrowDateText = tomorrowDateText,
                daySchedule = daySchedule,
                isClassLiveNow = false,
                isAllClassesForTodayCompleted = true,
                statusText = "No classes scheduled today • Self-Study",
                minutesRemaining = 0
            )
        }

        val active = nonFreeClasses.firstOrNull { it.status == SlotStatus.LIVE_NOW }
        val next = nonFreeClasses.firstOrNull { it.status == SlotStatus.UP_NEXT || it.status == SlotStatus.UPCOMING }

        val currentMin = currentTime.hour * 60 + currentTime.minute
        val statusText: String
        val minutesRemaining: Int
        val isAllCompleted = active == null && next == null

        if (active != null) {
            val rem = active.slot.endMinutes - currentMin
            statusText = "LIVE NOW • Ends in ${rem}m"
            minutesRemaining = rem
        } else if (next != null) {
            val diff = next.slot.startMinutes - currentMin
            statusText = if (diff <= 60) "UP NEXT • Starts in ${diff}m" else "Starts in ${diff / 60}h ${diff % 60}m"
            minutesRemaining = diff
        } else {
            statusText = if (tomorrowClass != null) {
                "All classes completed for today • Next lecture $tomorrowDateText"
            } else {
                "All classes completed for today"
            }
            minutesRemaining = 0
        }

        return ActiveClassInfo(
            activeClass = active,
            nextClass = next,
            tomorrowUpcomingClass = tomorrowClass,
            tomorrowDateText = tomorrowDateText,
            daySchedule = daySchedule,
            isClassLiveNow = active != null,
            isAllClassesForTodayCompleted = isAllCompleted,
            statusText = statusText,
            minutesRemaining = minutesRemaining
        )
    }

    companion object {
        private const val TAG = "ScheduleRepository"
        const val DEFAULT_CSV_ASSET = "timetable_data.csv"
        const val DEFAULT_REVISED_CSV_ASSET = "Schedule_MBA12_TERM II (Revised).csv"

        @Volatile
        private var INSTANCE: ScheduleRepository? = null

        fun getInstance(context: Context): ScheduleRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScheduleRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
