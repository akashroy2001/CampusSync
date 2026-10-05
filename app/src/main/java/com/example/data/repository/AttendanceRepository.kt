package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.entity.AttendanceLogEntity
import com.example.data.local.entity.CourseAttendanceEntity
import com.example.data.model.AttendanceZoneStatus
import com.example.data.model.CourseAttendance
import com.example.data.model.CourseAttendanceProgress
import com.example.data.model.DaySchedule
import com.example.data.model.MANDATORY_ATTENDANCE_THRESHOLD
import com.example.data.model.OverallAttendance
import com.example.data.model.OverallAttendanceProgress
import com.example.data.model.REQUIRED_ATTENDANCE_THRESHOLD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class AttendanceRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    val dao: AttendanceDao = db.attendanceDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Flow of courses from Room database mapped to detailed progress objects
    val coursesProgressFlow: Flow<List<CourseAttendanceProgress>> = dao.getAllCoursesFlow().map { entities ->
        entities.map { entity ->
            CourseAttendanceProgress(
                courseId = entity.id,
                courseName = entity.courseName,
                facultyName = "", // Professor name omitted as requested
                attendedClasses = entity.attendedClasses,
                totalConductedClasses = entity.totalConductedClasses,
                totalTermSessions = entity.totalTermSessions,
                requiredThreshold = entity.requiredThresholdPercentage,
                isLmsSynced = false,
                lastUpdatedTimestamp = entity.lastUpdatedTimestamp
            )
        }
    }

    val overallProgressFlow: Flow<OverallAttendanceProgress> = coursesProgressFlow.map { list ->
        calculateOverallProgress(list)
    }

    private val _attendance = MutableStateFlow(loadInitialAttendance())
    val attendance: StateFlow<OverallAttendance> = _attendance.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var latestSchedules: List<DaySchedule> = emptyList()

    init {
        scope.launch {
            initRoomDatabase()
        }
    }

    private suspend fun initRoomDatabase() {
        val existingCourses = dao.getAllCourses()
        val official = getOfficialTermCourses()

        // Remove any old/invalid courses that don't belong to the official Term II subjects
        existingCourses.forEach { ext ->
            val canonical = getCanonicalCourseName(ext.courseName)
            if (canonical == null) {
                dao.deleteCourse(ext)
            }
        }

        // Fetch fresh list after cleanup
        val currentInDb = dao.getAllCourses()

        // Insert or update all 10 official courses
        val entitiesToSave = official.map { off ->
            val match = currentInDb.firstOrNull { matchesCourse(it.courseName, off.courseName) }
            CourseAttendanceEntity(
                id = match?.id ?: 0,
                courseName = off.courseName,
                facultyName = "", // Professor name omitted
                attendedClasses = match?.attendedClasses ?: 0,
                totalConductedClasses = match?.totalConductedClasses ?: 0,
                totalTermSessions = off.totalTermSessions,
                requiredThresholdPercentage = MANDATORY_ATTENDANCE_THRESHOLD,
                isLmsSynced = false,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
        }

        dao.insertCourses(entitiesToSave)

        // Reactive collector keeps _attendance in sync with Room DB
        dao.getAllCoursesFlow().collect { entities ->
            if (entities.isNotEmpty()) {
                val domainCourses = entities.map { it.toDomain().copy(facultyName = "") }
                val totalAtt = domainCourses.sumOf { it.attendedClasses }
                val totalCond = domainCourses.sumOf { it.totalConductedClasses }
                _attendance.value = OverallAttendance(
                    totalAttended = totalAtt,
                    totalConducted = totalCond,
                    courses = domainCourses,
                    syncStatusText = "Term II Schedule Synced"
                )
            }
        }
    }

    fun calculateOverallProgress(courses: List<CourseAttendanceProgress>): OverallAttendanceProgress {
        val totalAtt = courses.sumOf { it.attendedClasses }
        val totalCond = courses.sumOf { it.totalConductedClasses }
        val safeCount = courses.count { it.zoneStatus == AttendanceZoneStatus.SAFE }
        val warnCount = courses.count { it.zoneStatus == AttendanceZoneStatus.WARNING }
        val critCount = courses.count { it.zoneStatus == AttendanceZoneStatus.CRITICAL }

        return OverallAttendanceProgress(
            totalAttended = totalAtt,
            totalConducted = totalCond,
            requiredThreshold = REQUIRED_ATTENDANCE_THRESHOLD,
            safeCoursesCount = safeCount,
            warningCoursesCount = warnCount,
            criticalCoursesCount = critCount,
            totalCoursesCount = courses.size
        )
    }

    /**
     * Synchronizes conducted class counts for each Term II subject based on
     * the timetable schedule for current day and all previous days.
     * Ensures all subjects with 6 or more scheduled classes are visible.
     */
    fun updateFromTimetable(
        schedules: List<DaySchedule>,
        currentDate: LocalDate = LocalDate.now()
    ) {
        latestSchedules = schedules
        val conductedCounts = mutableMapOf<String, Int>()
        val totalScheduledCounts = mutableMapOf<String, Int>()
        val dtf1 = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        for (day in schedules) {
            val parsedDate = try {
                LocalDate.parse(day.dateStr, dtf1)
            } catch (e: Exception) {
                try { LocalDate.parse(day.dateStr, dtf2) } catch (e2: Exception) { null }
            }

            for (item in day.classes) {
                if (item.isHoliday || item.isFreePeriod || item.courseName.isBlank()) continue
                val canonical = getCanonicalCourseName(item.courseName)
                if (canonical != null) {
                    totalScheduledCounts[canonical] = (totalScheduledCounts[canonical] ?: 0) + 1
                }
            }

            // Sync conducted count up to currentDate (current day and all previous classes)
            val isPastOrToday = parsedDate != null && !parsedDate.isAfter(currentDate)

            if (isPastOrToday) {
                for (item in day.classes) {
                    if (item.isHoliday || item.isFreePeriod || item.courseName.isBlank()) continue
                    val canonical = getCanonicalCourseName(item.courseName)
                    if (canonical != null) {
                        conductedCounts[canonical] = (conductedCounts[canonical] ?: 0) + 1
                    }
                }
            }
        }

        scope.launch {
            val existingEntities = dao.getAllCourses()
            val baseList = getOfficialTermCourses().toMutableList()

            // Include any additional subjects in schedule having 6 or more total classes
            totalScheduledCounts.forEach { (name, count) ->
                if (count >= 6 && baseList.none { matchesCourse(it.courseName, name) }) {
                    baseList.add(
                        CourseAttendance(
                            courseName = name,
                            facultyName = "",
                            attendedClasses = 0,
                            totalConductedClasses = 0,
                            totalTermSessions = count,
                            isLmsSynced = false
                        )
                    )
                }
            }

            val updatedEntities = baseList.map { official ->
                val conducted = conductedCounts[official.courseName] ?: 0
                val existing = existingEntities.firstOrNull { matchesCourse(it.courseName, official.courseName) }

                val attended = if (existing != null) {
                    if (existing.totalConductedClasses == 0 && conducted > 0) {
                        conducted // Default to full attendance for conducted classes
                    } else {
                        existing.attendedClasses.coerceIn(0, conducted)
                    }
                } else {
                    conducted
                }

                CourseAttendanceEntity(
                    id = existing?.id ?: 0,
                    courseName = official.courseName,
                    facultyName = "", // Professor name omitted
                    attendedClasses = attended,
                    totalConductedClasses = conducted,
                    totalTermSessions = official.totalTermSessions,
                    requiredThresholdPercentage = MANDATORY_ATTENDANCE_THRESHOLD,
                    isLmsSynced = false,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
            }

            dao.insertCourses(updatedEntities)

            val domainCourses = updatedEntities.map { it.toDomain() }
            val totalAtt = domainCourses.sumOf { it.attendedClasses }
            val totalCond = domainCourses.sumOf { it.totalConductedClasses }

            _attendance.value = OverallAttendance(
                totalAttended = totalAtt,
                totalConducted = totalCond,
                courses = domainCourses,
                syncStatusText = "Term II Schedule Synced"
            )
        }
    }

    /**
     * Sets the attended count for a subject (e.g. 2/2 or 1/2 present).
     * Automatically constrained to 0..totalConductedClasses.
     */
    fun setCourseAttendedCount(courseName: String, attended: Int) {
        scope.launch {
            val existingList = dao.getAllCourses()
            val entity = existingList.firstOrNull { matchesCourse(it.courseName, courseName) } ?: return@launch
            val clamped = attended.coerceIn(0, entity.totalConductedClasses)
            dao.updateAttendanceCounts(entity.id, clamped, entity.totalConductedClasses)

            dao.insertLog(
                AttendanceLogEntity(
                    courseId = entity.id,
                    courseName = entity.courseName,
                    dateStr = LocalDate.now().toString(),
                    wasAttended = clamped == entity.totalConductedClasses,
                    note = "$clamped/${entity.totalConductedClasses} present"
                )
            )

            // Update in-memory state immediately for instant UI responsiveness
            val currentList = _attendance.value.courses.map { c ->
                if (matchesCourse(c.courseName, courseName)) {
                    c.copy(attendedClasses = clamped)
                } else c
            }
            _attendance.value = _attendance.value.copy(
                totalAttended = currentList.sumOf { it.attendedClasses },
                totalConducted = currentList.sumOf { it.totalConductedClasses },
                courses = currentList
            )
        }
    }

    /**
     * Quick toggle / increment
     */
    fun markAttendance(courseName: String, isPresent: Boolean) {
        scope.launch {
            val existingList = dao.getAllCourses()
            val entity = existingList.firstOrNull { matchesCourse(it.courseName, courseName) } ?: return@launch
            val conducted = entity.totalConductedClasses.coerceAtLeast(1)
            val newAttended = if (isPresent) {
                (entity.attendedClasses + 1).coerceAtMost(conducted)
            } else {
                (entity.attendedClasses - 1).coerceAtLeast(0)
            }
            setCourseAttendedCount(entity.courseName, newAttended)
        }
    }

    fun markAttendance(courseId: Long, isPresent: Boolean) {
        scope.launch {
            val entity = dao.getCourseById(courseId) ?: return@launch
            markAttendance(entity.courseName, isPresent)
        }
    }

    fun updateCourseManual(courseName: String, attended: Int, conducted: Int) {
        scope.launch {
            val existingList = dao.getAllCourses()
            val entity = existingList.firstOrNull { matchesCourse(it.courseName, courseName) } ?: return@launch
            val safeCond = conducted.coerceAtLeast(0)
            val safeAtt = attended.coerceIn(0, safeCond)

            dao.updateAttendanceCounts(entity.id, safeAtt, safeCond)

            val currentList = _attendance.value.courses.map { c ->
                if (matchesCourse(c.courseName, courseName)) {
                    c.copy(attendedClasses = safeAtt, totalConductedClasses = safeCond)
                } else c
            }
            _attendance.value = _attendance.value.copy(
                totalAttended = currentList.sumOf { it.attendedClasses },
                totalConducted = currentList.sumOf { it.totalConductedClasses },
                courses = currentList
            )
        }
    }

    fun updateSingleSubject(courseName: String, attended: Int, conducted: Int) {
        updateCourseManual(courseName, attended, conducted)
    }

    fun updateCourseDetails(
        courseId: Long,
        attended: Int,
        conducted: Int,
        totalSessions: Int = 20,
        facultyName: String? = null
    ) {
        scope.launch {
            val entity = dao.getCourseById(courseId) ?: return@launch
            updateCourseManual(entity.courseName, attended, conducted)
        }
    }

    fun deleteCourse(courseId: Long) {
        scope.launch {
            val entity = dao.getCourseById(courseId) ?: return@launch
            deleteCourse(entity.courseName)
        }
    }

    fun deleteCourseByName(courseName: String) {
        deleteCourse(courseName)
    }

    fun deleteCourse(courseName: String) {
        scope.launch {
            val existingList = dao.getAllCourses()
            val entity = existingList.firstOrNull { matchesCourse(it.courseName, courseName) } ?: return@launch
            dao.deleteCourse(entity)
            val currentList = _attendance.value.courses.filter { !matchesCourse(it.courseName, courseName) }
            _attendance.value = _attendance.value.copy(
                totalAttended = currentList.sumOf { it.attendedClasses },
                totalConducted = currentList.sumOf { it.totalConductedClasses },
                courses = currentList
            )
        }
    }

    fun resetToOfficialTermCourses() {
        if (latestSchedules.isNotEmpty()) {
            updateFromTimetable(latestSchedules)
        } else {
            val official = getOfficialTermCourses()
            scope.launch {
                dao.deleteAllCourses()
                dao.insertCourses(official.map { CourseAttendanceEntity.fromDomain(it) })
                _attendance.value = OverallAttendance(
                    totalAttended = 0,
                    totalConducted = 0,
                    courses = official,
                    syncStatusText = "Term II Schedule Synced"
                )
            }
        }
    }

    private fun loadInitialAttendance(): OverallAttendance {
        val defaultList = getOfficialTermCourses()
        return OverallAttendance(
            totalAttended = 0,
            totalConducted = 0,
            courses = defaultList,
            syncStatusText = "MBA 2026-28 • Term II"
        )
    }

    /**
     * All subjects in Term II schedule that have 6 or more classes.
     * Total sessions from timetable:
     * - Marketing Management II: 20
     * - Macroeconomics: 20
     * - Operations Research: 20
     * - Financial Management I: 15
     * - Organizational Behaviour II: 10
     * - Design Thinking: 10
     * - Management Accounting II: 10
     * - Entrepreneurship: 10
     * - Human Resource Management: 10
     * - Workshops on Interviews and Presentations: 7
     */
    fun getOfficialTermCourses(): List<CourseAttendance> {
        return listOf(
            CourseAttendance("Marketing Management II", "", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 20, isLmsSynced = false),
            CourseAttendance("Macroeconomics", "", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 20, isLmsSynced = false),
            CourseAttendance("Operations Research", "", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 20, isLmsSynced = false),
            CourseAttendance("Financial Management I", "", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 15, isLmsSynced = false),
            CourseAttendance("Organizational Behaviour II", "", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 10, isLmsSynced = false),
            CourseAttendance("Design Thinking", "", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 10, isLmsSynced = false),
            CourseAttendance("Management Accounting II", "", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 10, isLmsSynced = false),
            CourseAttendance("Entrepreneurship", "", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 10, isLmsSynced = false),
            CourseAttendance("Human Resource Management", "", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 10, isLmsSynced = false),
            CourseAttendance("Workshops on Interviews and Presentations", "", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 7, isLmsSynced = false)
        )
    }

    /**
     * Maps raw or partial course strings from the schedule to the canonical subject name.
     * Prevents collisions between subjects containing similar words (e.g. Management, Macro).
     */
    fun getCanonicalCourseName(rawName: String): String? {
        val clean = cleanCourseTitle(rawName)
        val lower = clean.lowercase(Locale.ROOT).trim()
        return when {
            lower.contains("marketing") || lower.contains("mm ii") || lower.contains("mm-") -> "Marketing Management II"
            lower.contains("macro") -> "Macroeconomics"
            lower.contains("operations") || lower.contains("research") || lower == "or" -> "Operations Research"
            lower.contains("financial") || lower.contains("finance") || lower.contains("fm i") || lower.contains("fm-") -> "Financial Management I"
            lower.contains("organizational") || lower.contains("behaviour") || lower.contains("behavior") || lower.contains("ob ii") || lower.contains("ob-") -> "Organizational Behaviour II"
            lower.contains("design") || lower.contains("thinking") || lower == "dt" -> "Design Thinking"
            lower.contains("accounting") || lower.contains("management accounting") -> "Management Accounting II"
            lower.contains("entrepreneur") -> "Entrepreneurship"
            lower.contains("human resource") || lower.contains("resource management") || lower == "hrm" -> "Human Resource Management"
            lower.contains("workshop") || lower.contains("interview") || lower.contains("presentation") || lower == "wip" -> "Workshops on Interviews and Presentations"
            else -> null
        }
    }

    fun cleanCourseTitle(title: String): String {
        var clean = title.trim()
        val dashIndex = clean.lastIndexOf(" - ")
        if (dashIndex > 0) {
            clean = clean.substring(0, dashIndex).trim()
        }
        clean = clean.replace(Regex("(?i):\\s*Attendance.*"), "")
        clean = clean.replace(Regex("(?i)Attendance:?\\s*"), "")
        clean = clean.replace(Regex("(?i)\\bTerm\\s*(I|II|1|2)\\b"), "").trim()
        clean = clean.replace(Regex("[\\(\\)\\[\\]]"), "").trim()
        return clean.ifBlank { title.trim() }
    }

    fun matchesCourse(name1: String, name2: String): Boolean {
        val c1 = getCanonicalCourseName(name1)
        val c2 = getCanonicalCourseName(name2)
        if (c1 != null && c2 != null) {
            return c1 == c2
        }
        val clean1 = cleanCourseTitle(name1).lowercase(Locale.ROOT)
        val clean2 = cleanCourseTitle(name2).lowercase(Locale.ROOT)
        return clean1 == clean2
    }

    companion object {
        private const val PREFS_NAME = "campussync_attendance_prefs"

        @Volatile
        private var INSTANCE: AttendanceRepository? = null

        fun getInstance(context: Context): AttendanceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AttendanceRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
