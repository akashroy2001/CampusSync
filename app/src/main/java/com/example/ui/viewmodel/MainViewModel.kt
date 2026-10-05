package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DayMessMenu
import com.example.data.model.DaySchedule
import com.example.data.model.OverallAttendance
import com.example.data.model.SlotStatus
import com.example.data.notification.NotificationHelper
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.MessMenuRepository
import com.example.data.repository.ScheduleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class ScheduleViewMode {
    TODAYS_AGENDA,
    FULL_WEEK
}

enum class NavigationTab {
    SCHEDULE,
    MESS,
    NOW_DASHBOARD,
    ATTENDANCE
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferencesManager.getInstance(application)
    private val scheduleRepo = ScheduleRepository.getInstance(application)
    private val attendanceRepo = AttendanceRepository.getInstance(application)

    val notificationsEnabled: StateFlow<Boolean> = prefs.notificationsEnabled
    val isLoadingSchedule: StateFlow<Boolean> = scheduleRepo.isLoading
    val attendance: StateFlow<OverallAttendance> = attendanceRepo.attendance
    val isAttendanceSyncing: StateFlow<Boolean> = attendanceRepo.isSyncing

    // Active bottom navigation tab
    private val _currentTab = MutableStateFlow(NavigationTab.NOW_DASHBOARD)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    // Real-time synchronization to current system date and time
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _currentTime = MutableStateFlow(LocalTime.now())
    val currentTime: StateFlow<LocalTime> = _currentTime.asStateFlow()

    private val _scheduleViewMode = MutableStateFlow(ScheduleViewMode.TODAYS_AGENDA)
    val scheduleViewMode: StateFlow<ScheduleViewMode> = _scheduleViewMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Mess menu selected day index (0 = Monday .. 6 = Sunday)
    // If today's meals have all concluded (past 10 PM), default to tomorrow!
    private val initialMessDayIndex: Int = run {
        val now = LocalTime.now()
        val today = LocalDate.now()
        val currentMin = now.hour * 60 + now.minute
        val todayIdx = today.dayOfWeek.value - 1
        val mealStatus = MessMenuRepository.getCurrentMealStatus(currentMin, todayIdx)
        if (mealStatus.isAllTodayMealsEnded) {
            (todayIdx + 1) % 7
        } else {
            todayIdx
        }
    }
    private val _selectedMessDayIndex = MutableStateFlow(initialMessDayIndex)
    val selectedMessDayIndex: StateFlow<Int> = _selectedMessDayIndex.asStateFlow()

    // UI feedback state (e.g. snackbars)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private var timeTickerJob: Job? = null

    init {
        // Initialize Notification Channel
        NotificationHelper.initChannel(application)

        // Load schedule from default asset or custom imported file
        viewModelScope.launch {
            scheduleRepo.loadSchedule()
            scheduleRemindersForToday()
        }

        viewModelScope.launch {
            scheduleRepo.schedules.collect { list ->
                if (list.isNotEmpty()) {
                    attendanceRepo.updateFromTimetable(list)
                }
            }
        }

        startTimeTicker()
    }

    private fun startTimeTicker() {
        timeTickerJob?.cancel()
        timeTickerJob = viewModelScope.launch {
            while (true) {
                _currentTime.value = LocalTime.now()
                delay(10_000L) // update every 10 seconds for real-time live accuracy
            }
        }
    }

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun jumpToToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun setScheduleViewMode(mode: ScheduleViewMode) {
        _scheduleViewMode.value = mode
    }

    fun setSelectedMessDayIndex(index: Int) {
        _selectedMessDayIndex.value = (index % 7 + 7) % 7
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }

    fun toggleNotifications(enabled: Boolean) {
        prefs.setNotificationsEnabled(enabled)
        if (enabled) {
            scheduleRemindersForToday()
            _feedbackMessage.value = "Class reminders enabled (10m before start)"
        } else {
            _feedbackMessage.value = "Class reminders turned off"
        }
    }

    fun sendTestNotification() {
        NotificationHelper.sendTestNotification(getApplication())
        _feedbackMessage.value = "Sample notification triggered"
    }

    fun importNewSpreadsheet(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                var fileName = "schedule.csv"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex) ?: "schedule.csv"
                    }
                }

                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _feedbackMessage.value = "Error: Could not open selected file."
                    return@launch
                }
                val result = scheduleRepo.saveCustomSchedule(inputStream, fileName)
                result.onSuccess { msg ->
                    _feedbackMessage.value = msg
                    scheduleRemindersForToday()
                }.onFailure { err ->
                    _feedbackMessage.value = "Import failed: ${err.message}"
                }
            } catch (e: Exception) {
                _feedbackMessage.value = "Error reading file: ${e.message}"
            }
        }
    }

    fun resetScheduleToDefault() {
        viewModelScope.launch {
            val result = scheduleRepo.resetToDefault()
            result.onSuccess {
                _feedbackMessage.value = "Timetable reset to official Term I schedule"
                scheduleRemindersForToday()
            }.onFailure {
                _feedbackMessage.value = "Failed to reset timetable: ${it.message}"
            }
        }
    }

    fun isUsingCustomSchedule(): Boolean {
        return scheduleRepo.isCustomScheduleActive()
    }

    private fun scheduleRemindersForToday() {
        if (!notificationsEnabled.value) return
        val daySched = scheduleRepo.getUpdatedDaySchedule(
            targetDate = _selectedDate.value,
            currentTime = _currentTime.value
        )
        NotificationHelper.scheduleTodayReminders(getApplication(), daySched)
    }

    // All schedules for full week/term view, with real-time status mapped per date
    val allSchedules: StateFlow<List<DaySchedule>> = combine(
        scheduleRepo.schedules,
        currentTime
    ) { schedules, time ->
        val today = LocalDate.now()
        val dtf1 = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val currentMin = time.hour * 60 + time.minute

        schedules.map { day ->
            val parsedDate = try {
                LocalDate.parse(day.dateStr, dtf1)
            } catch (e: Exception) {
                try { LocalDate.parse(day.dateStr, dtf2) } catch (e2: Exception) { null }
            }

            val isFuture = parsedDate?.isAfter(today) ?: false
            val isPast = parsedDate?.isBefore(today) ?: false
            val isToday = parsedDate?.isEqual(today) ?: day.dayName.equals(today.dayOfWeek.name, ignoreCase = true)

            val updatedClasses = day.classes.map { item ->
                if (item.isHoliday) {
                    item.copy(status = SlotStatus.HOLIDAY)
                } else if (item.isFreePeriod) {
                    item.copy(status = SlotStatus.FREE_PERIOD)
                } else {
                    val start = item.slot.startMinutes
                    val end = item.slot.endMinutes
                    val status = when {
                        isFuture -> SlotStatus.UPCOMING
                        isPast -> SlotStatus.COMPLETED
                        isToday && currentMin in start..end -> SlotStatus.LIVE_NOW
                        isToday && currentMin < start && (start - currentMin) <= 60 -> SlotStatus.UP_NEXT
                        isToday && currentMin < start -> SlotStatus.UPCOMING
                        isToday -> SlotStatus.COMPLETED
                        else -> SlotStatus.UPCOMING
                    }
                    val minUntilStart = if (isToday) (start - currentMin).coerceAtLeast(0) else 0
                    val minUntilEnd = if (isToday) (end - currentMin).coerceAtLeast(0) else 0
                    item.copy(
                        status = status,
                        minutesUntilStart = minUntilStart,
                        minutesUntilEnd = minUntilEnd
                    )
                }
            }
            day.copy(classes = updatedClasses)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Updated schedule for the selected date in Timetable tab
    val currentDaySchedule: StateFlow<DaySchedule?> = combine(
        scheduleRepo.schedules,
        selectedDate,
        currentTime
    ) { _, date, time ->
        scheduleRepo.getUpdatedDaySchedule(date, time)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active class info for "Now" Dashboard
    // CRITICAL: The "Now" Dashboard is strictly rooted in TODAY (LocalDate.now())
    // and must NEVER change when an upcoming date is selected in the Timetable tab.
    val activeClassInfo: StateFlow<ScheduleRepository.ActiveClassInfo?> = combine(
        scheduleRepo.schedules,
        currentTime
    ) { _, time ->
        scheduleRepo.getActiveClassInfo(LocalDate.now(), time)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current meal status for "Now" Dashboard & Mess Menu
    // CRITICAL: Must strictly be based on today's actual day of week and current time!
    val currentMealStatus: StateFlow<MessMenuRepository.CurrentMealStatus> = currentTime.map { time ->
        val currentMin = time.hour * 60 + time.minute
        val todayIdx = LocalDate.now().dayOfWeek.value - 1
        MessMenuRepository.getCurrentMealStatus(currentMin, todayIdx)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MessMenuRepository.getCurrentMealStatus(
            LocalTime.now().hour * 60 + LocalTime.now().minute,
            (LocalDate.now().dayOfWeek.value - 1)
        )
    )

    // Mess menu for the currently swiped/selected day
    val selectedDayMessMenu: StateFlow<DayMessMenu> = selectedMessDayIndex.combine(currentMealStatus) { idx, _ ->
        MessMenuRepository.getMenuForDayIndex(idx)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MessMenuRepository.getMenuForDayIndex(initialMessDayIndex)
    )

    fun syncAttendanceWithLms() {
        attendanceRepo.triggerLmsSync { success ->
            if (success) {
                _feedbackMessage.value = "Attendance refreshed with LMS portal"
            }
        }
    }

    fun processLmsAttendancePayload(jsonPayload: String, studentName: String? = null): Boolean {
        val success = attendanceRepo.processLmsExtractedAttendance(jsonPayload, studentName)
        if (success) {
            _feedbackMessage.value = "Attendance successfully updated from LMS!"
        }
        return success
    }

    fun checkDailyAttendanceRefresh() {
        attendanceRepo.checkDailyRefresh()
    }

    fun resetAttendanceToOfficial() {
        attendanceRepo.resetToOfficialTermCourses()
        _feedbackMessage.value = "Restored attendance for all 8 subjects"
    }

    fun updateSingleSubjectAttendance(courseName: String, attended: Int, conducted: Int) {
        attendanceRepo.updateSingleSubject(courseName, attended, conducted)
        _feedbackMessage.value = "Updated $courseName attendance"
    }

    fun recordAttendance(courseName: String, attended: Boolean) {
        viewModelScope.launch {
            val course = attendance.value.courses.firstOrNull { it.courseName == courseName }
            if (course != null) {
                val entity = attendanceRepo.dao.getCourseByName(courseName)
                if (entity != null) {
                    attendanceRepo.markAttendance(entity.id, attended)
                } else {
                    val newAtt = if (attended) course.attendedClasses + 1 else course.attendedClasses
                    val newCond = course.totalConductedClasses + 1
                    attendanceRepo.updateSingleSubject(courseName, newAtt, newCond)
                }
                _feedbackMessage.value = if (attended) "Marked Present for $courseName" else "Marked Absent for $courseName"
            }
        }
    }

    fun addNewCourse(courseName: String, facultyName: String, attended: Int, conducted: Int, totalSessions: Int) {
        viewModelScope.launch {
            attendanceRepo.addCourse(courseName, facultyName, attended, conducted, totalSessions)
            _feedbackMessage.value = "Added course: $courseName"
        }
    }

    fun deleteCourse(courseName: String) {
        viewModelScope.launch {
            val entity = attendanceRepo.dao.getCourseByName(courseName)
            if (entity != null) {
                attendanceRepo.deleteCourse(entity.id)
                _feedbackMessage.value = "Deleted course: $courseName"
            }
        }
    }
}
