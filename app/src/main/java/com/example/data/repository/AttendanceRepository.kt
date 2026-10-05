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
                facultyName = entity.facultyName,
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
        val hasOldTerm1Courses = existingCourses.any {
            it.courseName.contains("Microeconomics") ||
            it.courseName.contains("Sustainable") ||
            it.courseName.contains("Information Technology") ||
            it.courseName.contains("Written Analysis") ||
            it.courseName.contains("Statistics") ||
            it.courseName.contains("Marketing Management I") ||
            it.courseName.contains("Management Accounting I") ||
            it.courseName.contains("Organizational Behaviour I")
        }

        if (existingCourses.isEmpty() || hasOldTerm1Courses) {
            dao.deleteAllCourses()
            val term2Courses = getOfficialTermCourses()
            dao.insertCourses(term2Courses.map { CourseAttendanceEntity.fromDomain(it) })
        }

        // Keep _attendance updated reactively from Room DB
        dao.getAllCoursesFlow().collect { entities ->
            if (entities.isNotEmpty()) {
                val domainCourses = entities.map { it.toDomain() }
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
     */
    fun updateFromTimetable(
        schedules: List<DaySchedule>,
        currentDate: LocalDate = LocalDate.now()
    ) {
        latestSchedules = schedules
        val courseStats = mutableMapOf<String, Pair<Int, String>>()
        val dtf1 = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        for (day in schedules) {
            val parsedDate = try {
                LocalDate.parse(day.dateStr, dtf1)
            } catch (e: Exception) {
                try { LocalDate.parse(day.dateStr, dtf2) } catch (e2: Exception) { null }
            }

            // Sync with current day and all previous classes of that subject
            val isPastOrToday = parsedDate != null && !parsedDate.isAfter(currentDate)

            if (isPastOrToday) {
                for (item in day.classes) {
                    if (item.isHoliday || item.isFreePeriod || item.courseName.isBlank()) continue
                    val cleanName = cleanCourseTitle(item.courseName)
                    val existing = courseStats[cleanName] ?: (0 to item.facultyName)
                    val faculty = if (existing.second.isNotBlank()) existing.second else item.facultyName
                    courseStats[cleanName] = (existing.first + 1 to faculty)
                }
            }
        }

        scope.launch {
            val existingEntities = dao.getAllCourses()
            val officialCourses = getOfficialTermCourses()

            val updatedEntities = officialCourses.map { official ->
                val stats = courseStats.entries.firstOrNull { matchesCourse(it.key, official.courseName) }?.value
                val conducted = stats?.first ?: 0
                val faculty = if (!stats?.second.isNullOrBlank()) stats!!.second else official.facultyName

                val existing = existingEntities.firstOrNull { matchesCourse(it.courseName, official.courseName) }

                val attended = if (existing != null) {
                    if (existing.totalConductedClasses == 0 && conducted > 0) {
                        // Newly conducted classes default to full attendance
                        conducted
                    } else {
                        // Preserve user's manual selection, bounded between 0 and conducted
                        existing.attendedClasses.coerceIn(0, conducted)
                    }
                } else {
                    conducted
                }

                CourseAttendanceEntity(
                    id = existing?.id ?: 0,
                    courseName = official.courseName,
                    facultyName = faculty,
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
            val entity = dao.getCourseByName(courseName) ?: return@launch
            val clamped = attended.coerceIn(0, entity.totalConductedClasses)
            dao.updateAttendanceCounts(entity.id, clamped, entity.totalConductedClasses)

            dao.insertLog(
                AttendanceLogEntity(
                    courseId = entity.id,
                    courseName = courseName,
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
     * Update attended and conducted numbers directly from edit dialog.
     */
    fun updateSingleSubject(courseName: String, attended: Int, conducted: Int, faculty: String = "") {
        scope.launch {
            val existing = dao.getCourseByName(courseName)
            val cleanCond = conducted.coerceAtLeast(0)
            val cleanAtt = attended.coerceIn(0, cleanCond)

            if (existing != null) {
                val updated = existing.copy(
                    attendedClasses = cleanAtt,
                    totalConductedClasses = cleanCond,
                    facultyName = if (faculty.isNotBlank()) faculty else existing.facultyName,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
                dao.updateCourse(updated)
            } else {
                dao.insertCourse(
                    CourseAttendanceEntity(
                        courseName = courseName,
                        facultyName = faculty,
                        attendedClasses = cleanAtt,
                        totalConductedClasses = cleanCond,
                        totalTermSessions = 20,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                )
            }

            val currentList = _attendance.value.courses.map { c ->
                if (matchesCourse(c.courseName, courseName)) {
                    c.copy(
                        attendedClasses = cleanAtt,
                        totalConductedClasses = cleanCond,
                        facultyName = if (faculty.isNotBlank()) faculty else c.facultyName
                    )
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
     * Quick mark attendance (+1 attended if present, +1 conducted).
     */
    suspend fun markAttendance(courseId: Long, attended: Boolean, note: String = "") {
        withContext(Dispatchers.IO) {
            val entity = dao.getCourseById(courseId) ?: return@withContext
            val newAttended = if (attended) entity.attendedClasses + 1 else entity.attendedClasses
            val newConducted = entity.totalConductedClasses + 1
            dao.updateAttendanceCounts(courseId, newAttended, newConducted)

            val todayStr = LocalDate.now().toString()
            dao.insertLog(
                AttendanceLogEntity(
                    courseId = courseId,
                    courseName = entity.courseName,
                    dateStr = todayStr,
                    wasAttended = attended,
                    note = note
                )
            )
        }
    }

    suspend fun updateCourseDetails(
        courseId: Long,
        attended: Int,
        conducted: Int,
        totalSessions: Int = 20,
        facultyName: String? = null
    ) {
        withContext(Dispatchers.IO) {
            val entity = dao.getCourseById(courseId) ?: return@withContext
            val updated = entity.copy(
                attendedClasses = attended.coerceIn(0, conducted),
                totalConductedClasses = conducted.coerceAtLeast(0),
                totalTermSessions = totalSessions,
                facultyName = facultyName ?: entity.facultyName,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            dao.updateCourse(updated)
        }
    }

    suspend fun addCourse(
        courseName: String,
        facultyName: String = "",
        attended: Int = 0,
        conducted: Int = 0,
        totalSessions: Int = 20
    ): Long {
        return withContext(Dispatchers.IO) {
            val entity = CourseAttendanceEntity(
                courseName = cleanCourseTitle(courseName),
                facultyName = facultyName,
                attendedClasses = attended.coerceIn(0, conducted),
                totalConductedClasses = conducted.coerceAtLeast(0),
                totalTermSessions = totalSessions,
                isLmsSynced = false
            )
            dao.insertCourse(entity)
        }
    }

    suspend fun deleteCourse(courseId: Long) {
        withContext(Dispatchers.IO) {
            dao.deleteCourseById(courseId)
        }
    }

    fun deleteCourseByName(courseName: String) {
        scope.launch {
            val entity = dao.getCourseByName(courseName)
            if (entity != null) {
                dao.deleteCourseById(entity.id)
            }
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
            }
            _attendance.value = OverallAttendance(
                totalAttended = 0,
                totalConducted = 0,
                courses = official,
                syncStatusText = "Term II Attendance Reset"
            )
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

    fun getOfficialTermCourses(): List<CourseAttendance> {
        return listOf(
            CourseAttendance("Marketing Management II", "Prof. Chandan Parsad", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 20, isLmsSynced = false),
            CourseAttendance("Workshops on Interviews and Presentations", "Prof. Anamita Guha", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 7, isLmsSynced = false),
            CourseAttendance("Macroeconomics", "Prof. Gupteswar Patel", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 20, isLmsSynced = false),
            CourseAttendance("Organizational Behaviour II", "Prof. Sudipt Kumar", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 10, isLmsSynced = false),
            CourseAttendance("Financial Management I", "Prof. Bharati Singh", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 15, isLmsSynced = false),
            CourseAttendance("Operations Research", "Prof. Rohit Agrawal", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 20, isLmsSynced = false),
            CourseAttendance("Design Thinking", "Prof. Bishal Dey Sarkar", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 10, isLmsSynced = false),
            CourseAttendance("Management Accounting II", "Prof. Archana Patro", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 10, isLmsSynced = false),
            CourseAttendance("Entrepreneurship", "Prof. Sunil Kumar Yadav", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 10, isLmsSynced = false),
            CourseAttendance("Human Resource Management", "Prof. Abhyudaya Anand Mishra", attendedClasses = 0, totalConductedClasses = 0, totalTermSessions = 10, isLmsSynced = false)
        )
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

    fun matchesCourse(canonicalName: String, otherName: String): Boolean {
        val c1 = canonicalName.lowercase(Locale.ROOT).trim()
        val c2 = otherName.lowercase(Locale.ROOT).trim()
        if (c1 == c2) return true
        if (c1.contains(c2) || c2.contains(c1)) return true

        fun norm(s: String) = s.replace(Regex("[^a-z0-9]"), "")
        val n1 = norm(c1)
        val n2 = norm(c2)
        if (n1 == n2 || n1.contains(n2) || n2.contains(n1)) return true

        // MBA Term II Specific Domain Aliases
        val isMkt1 = c1.contains("marketing") || c1.contains("mm")
        val isMkt2 = c2.contains("marketing") || c2.contains("mm")
        if (isMkt1 && isMkt2) return true

        val isWip1 = c1.contains("workshop") || c1.contains("interview") || c1.contains("presentation") || c1.contains("wip")
        val isWip2 = c2.contains("workshop") || c2.contains("interview") || c2.contains("presentation") || c2.contains("wip")
        if (isWip1 && isWip2) return true

        val isMacro1 = c1.contains("macro")
        val isMacro2 = c2.contains("macro")
        if (isMacro1 && isMacro2) return true

        val isOb1 = (c1.contains("behav") || c1.contains("ob"))
        val isOb2 = (c2.contains("behav") || c2.contains("ob"))
        if (isOb1 && isOb2) return true

        val isFm1 = c1.contains("financial") || c1.contains("fm")
        val isFm2 = c2.contains("financial") || c2.contains("fm")
        if (isFm1 && isFm2) return true

        val isOr1 = c1.contains("operations") || c1.contains("research") || c1 == "or"
        val isOr2 = c2.contains("operations") || c2.contains("research") || c2 == "or"
        if (isOr1 && isOr2) return true

        val isDt1 = c1.contains("design") || c1.contains("thinking") || c1 == "dt"
        val isDt2 = c2.contains("design") || c2.contains("thinking") || c2 == "dt"
        if (isDt1 && isDt2) return true

        val isMa1 = c1.contains("accounting") || c1.contains("ma")
        val isMa2 = c2.contains("accounting") || c2.contains("ma")
        if (isMa1 && isMa2) return true

        val isEnt1 = c1.contains("entrepreneur") || c1.contains("entre")
        val isEnt2 = c2.contains("entrepreneur") || c2.contains("entre")
        if (isEnt1 && isEnt2) return true

        val isHrm1 = c1.contains("human resource") || c1.contains("hrm") || c1.contains("hr")
        val isHrm2 = c2.contains("human resource") || c2.contains("hrm") || c2.contains("hr")
        if (isHrm1 && isHrm2) return true

        return false
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
