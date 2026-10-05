package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AttendanceZoneStatus
import com.example.data.model.CourseAttendanceProgress
import com.example.data.model.OverallAttendanceProgress
import com.example.data.model.REQUIRED_ATTENDANCE_THRESHOLD
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AttendanceFilter {
    ALL,
    SAFE,       // >= 80% with buffer
    AT_RISK,    // On the brink (0 bunks left)
    CRITICAL    // < 80% shortage
}

data class AttendanceUiState(
    val allCourses: List<CourseAttendanceProgress> = emptyList(),
    val filteredCourses: List<CourseAttendanceProgress> = emptyList(),
    val overall: OverallAttendanceProgress = OverallAttendanceProgress(0, 0),
    val safeCount: Int = 0,
    val warningCount: Int = 0,
    val criticalCount: Int = 0,
    val selectedFilter: AttendanceFilter = AttendanceFilter.ALL,
    val searchQuery: String = "",
    val isSyncing: Boolean = false,
    val feedbackMessage: String? = null
)

/**
 * ViewModel to track course attendance percentages against the mandatory 80% safe zone threshold.
 * Backed by Room Database via AttendanceRepository.
 */
class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AttendanceRepository.getInstance(application)

    private val _selectedFilter = MutableStateFlow(AttendanceFilter.ALL)
    val selectedFilter: StateFlow<AttendanceFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    val isSyncing: StateFlow<Boolean> = repository.isSyncing

    private val filterAndQuery = combine(_selectedFilter, _searchQuery) { filter, query ->
        Pair(filter, query)
    }

    private val syncAndFeedback = combine(repository.isSyncing, _feedbackMessage) { syncing, feedback ->
        Pair(syncing, feedback)
    }

    val uiState: StateFlow<AttendanceUiState> = combine(
        repository.coursesProgressFlow,
        repository.overallProgressFlow,
        filterAndQuery,
        syncAndFeedback
    ) { courses, overall, filterQueryPair, syncFeedbackPair ->
        val (filter, query) = filterQueryPair
        val (syncing, feedback) = syncFeedbackPair

        val safe = courses.count { it.zoneStatus == AttendanceZoneStatus.SAFE }
        val warning = courses.count { it.zoneStatus == AttendanceZoneStatus.WARNING }
        val critical = courses.count { it.zoneStatus == AttendanceZoneStatus.CRITICAL }

        val filtered = courses.filter { course ->
            val matchesFilter = when (filter) {
                AttendanceFilter.ALL -> true
                AttendanceFilter.SAFE -> course.zoneStatus == AttendanceZoneStatus.SAFE
                AttendanceFilter.AT_RISK -> course.zoneStatus == AttendanceZoneStatus.WARNING
                AttendanceFilter.CRITICAL -> course.zoneStatus == AttendanceZoneStatus.CRITICAL
            }
            val matchesQuery = query.isBlank() ||
                course.courseName.contains(query, ignoreCase = true) ||
                course.facultyName.contains(query, ignoreCase = true)

            matchesFilter && matchesQuery
        }

        AttendanceUiState(
            allCourses = courses,
            filteredCourses = filtered,
            overall = overall,
            safeCount = safe,
            warningCount = warning,
            criticalCount = critical,
            selectedFilter = filter,
            searchQuery = query,
            isSyncing = syncing,
            feedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AttendanceUiState()
    )

    fun setFilter(filter: AttendanceFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    /**
     * Records attendance for a course (+1 conducted, and +1 attended if present).
     * Saves directly to Room database with audit log.
     */
    fun recordAttendance(courseId: Long, attended: Boolean) {
        viewModelScope.launch {
            repository.markAttendance(courseId, attended)
            _feedbackMessage.value = if (attended) {
                "Marked Present! Attendance recorded."
            } else {
                "Marked Absent. Attendance updated."
            }
        }
    }

    fun recordPresent(courseId: Long) = recordAttendance(courseId, true)

    fun recordAbsent(courseId: Long) = recordAttendance(courseId, false)

    /**
     * Updates manual numbers for attended, conducted, and total term sessions.
     */
    fun updateCourseAttendance(
        courseId: Long,
        attended: Int,
        conducted: Int,
        totalSessions: Int = 20,
        facultyName: String? = null
    ) {
        viewModelScope.launch {
            repository.updateCourseDetails(
                courseId = courseId,
                attended = attended,
                conducted = conducted,
                totalSessions = totalSessions,
                facultyName = facultyName
            )
            _feedbackMessage.value = "Course attendance saved to database"
        }
    }

    /**
     * Adds a new custom or elective course to the Room database.
     */
    fun addCourse(
        courseName: String,
        facultyName: String = "",
        attended: Int = 0,
        conducted: Int = 0,
        totalSessions: Int = 20
    ) {
        viewModelScope.launch {
            if (courseName.isBlank()) {
                _feedbackMessage.value = "Course name cannot be empty"
                return@launch
            }
            repository.addCourse(
                courseName = courseName,
                facultyName = facultyName,
                attended = attended,
                conducted = conducted,
                totalSessions = totalSessions
            )
            _feedbackMessage.value = "Added course: $courseName"
        }
    }

    /**
     * Deletes a course from Room database.
     */
    fun deleteCourse(courseId: Long) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
            _feedbackMessage.value = "Course removed"
        }
    }

    /**
     * Resets Room DB to standard official Term courses.
     */
    fun resetToOfficialCourses() {
        viewModelScope.launch {
            repository.resetRoomToOfficialCourses()
            _feedbackMessage.value = "Restored all 8 official courses to database"
        }
    }

    /**
     * Triggers LMS portal refresh.
     */
    fun syncWithLms() {
        repository.triggerLmsSync { success ->
            _feedbackMessage.value = if (success) {
                "LMS attendance synced"
            } else {
                "LMS refresh failed"
            }
        }
    }

    /**
     * Processes JSON payload extracted from LMS and updates Room database.
     */
    fun processLmsPayload(json: String, studentName: String? = null): Boolean {
        val success = repository.processLmsExtractedAttendance(json, studentName)
        if (success) {
            _feedbackMessage.value = "Successfully synced attendance from LMS into database"
        }
        return success
    }
}
