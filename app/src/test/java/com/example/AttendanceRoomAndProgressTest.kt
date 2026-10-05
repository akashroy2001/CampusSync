package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.entity.CourseAttendanceEntity
import com.example.data.model.AttendanceZoneStatus
import com.example.data.model.CourseAttendanceProgress
import com.example.data.model.OverallAttendanceProgress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AttendanceRoomAndProgressTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AttendanceDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.attendanceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAttendanceProgressCalculations_safeZoneAbove80() {
        // 12 attended out of 14 conducted = 85.71%
        val course = CourseAttendanceProgress(
            courseName = "Marketing Management II",
            facultyName = "Prof. Chandan Parsad",
            attendedClasses = 12,
            totalConductedClasses = 14,
            totalTermSessions = 20
        )

        assertTrue(course.isInSafeZone)
        assertTrue(course.currentPercentage >= 80.0f)
        assertEquals(1, course.safeBunksRemaining) // 12 / 0.8 = 15; 15 - 14 = 1 bunk left
        assertEquals(0, course.classesNeededToRecover)
        assertEquals(AttendanceZoneStatus.SAFE, course.zoneStatus)
        assertTrue(course.thresholdDelta > 0)
    }

    @Test
    fun testAttendanceProgressCalculations_deficitBelow80() {
        // 11 attended out of 15 conducted = 73.33%
        val course = CourseAttendanceProgress(
            courseName = "Operations Research",
            facultyName = "Prof. Rohit Agrawal",
            attendedClasses = 11,
            totalConductedClasses = 15,
            totalTermSessions = 20
        )

        assertFalse(course.isInSafeZone)
        assertTrue(course.currentPercentage < 80.0f)
        assertEquals(0, course.safeBunksRemaining)
        // Recovery needed: 4 * 15 - 5 * 11 = 60 - 55 = 5 classes
        assertEquals(5, course.classesNeededToRecover)
        assertEquals(AttendanceZoneStatus.CRITICAL, course.zoneStatus)
        assertTrue(course.thresholdDelta < 0)
    }

    @Test
    fun testAttendanceProgressCalculations_onTheBrinkWarning() {
        // 8 attended out of 10 conducted = exactly 80.0%
        val course = CourseAttendanceProgress(
            courseName = "Organizational Behaviour II",
            facultyName = "Prof. Sudipt Kumar",
            attendedClasses = 8,
            totalConductedClasses = 10,
            totalTermSessions = 10
        )

        assertTrue(course.isInSafeZone)
        assertEquals(80.0f, course.currentPercentage, 0.01f)
        // 8 / 0.8 = 10; 10 - 10 = 0 bunks left
        assertEquals(0, course.safeBunksRemaining)
        assertEquals(AttendanceZoneStatus.WARNING, course.zoneStatus)
    }

    @Test
    fun testRoomDatabaseOperations() = runBlocking {
        val course1 = CourseAttendanceEntity(
            courseName = "Macroeconomics",
            facultyName = "Prof. Gupteswar Patel",
            attendedClasses = 13,
            totalConductedClasses = 14,
            totalTermSessions = 20
        )
        val course2 = CourseAttendanceEntity(
            courseName = "Design Thinking",
            facultyName = "Prof. Bishal Dey Sarkar",
            attendedClasses = 0,
            totalConductedClasses = 0,
            totalTermSessions = 10
        )

        dao.insertCourses(listOf(course1, course2))

        val count = dao.getCourseCount()
        assertEquals(2, count)

        val retrieved = dao.getCourseByName("Macroeconomics")
        assertEquals(13, retrieved?.attendedClasses)
        assertEquals(14, retrieved?.totalConductedClasses)

        // Mark attendance: +1 attended, +1 conducted
        dao.updateAttendanceCounts(retrieved!!.id, 14, 15)
        val updated = dao.getCourseById(retrieved.id)
        assertEquals(14, updated?.attendedClasses)
        assertEquals(15, updated?.totalConductedClasses)

        // Test Flow
        val allFlow = dao.getAllCoursesFlow().first()
        assertEquals(2, allFlow.size)
    }

    @Test
    fun testOverallAttendanceProgress() {
        val overall = OverallAttendanceProgress(
            totalAttended = 74,
            totalConducted = 87,
            safeCoursesCount = 6,
            warningCoursesCount = 1,
            criticalCoursesCount = 1,
            totalCoursesCount = 8
        )

        // 74 / 87 = 85.05%
        assertTrue(overall.isInSafeZone)
        assertTrue(overall.overallPercentage >= 80.0f)
        assertEquals(13, overall.totalMissed)
        assertTrue(overall.overallSafeBunks >= 0)
    }

    @Test
    fun testAllTermIISubjectsWith6OrMoreClassesArePresentAndNoCollisions() {
        val repo = com.example.data.repository.AttendanceRepository(ApplicationProvider.getApplicationContext())
        val officialCourses = repo.getOfficialTermCourses()

        // All 10 curriculum subjects with 6+ scheduled sessions must be present
        val expectedSubjects = listOf(
            "Marketing Management II",
            "Macroeconomics",
            "Operations Research",
            "Financial Management I",
            "Organizational Behaviour II",
            "Design Thinking",
            "Management Accounting II",
            "Entrepreneurship",
            "Human Resource Management",
            "Workshops on Interviews and Presentations"
        )

        assertEquals(10, officialCourses.size)
        expectedSubjects.forEach { expected ->
            val match = officialCourses.find { it.courseName == expected }
            assertTrue("Subject $expected must be in official courses", match != null)
            assertTrue("Sessions for $expected should be >= 6", match!!.totalTermSessions >= 6)
            assertTrue("Professor name must be omitted for $expected", match.facultyName.isEmpty())
        }

        // Verify canonical matching has no cross-subject collisions
        for (i in expectedSubjects.indices) {
            for (j in i + 1 until expectedSubjects.size) {
                val s1 = expectedSubjects[i]
                val s2 = expectedSubjects[j]
                assertFalse("Collision between '$s1' and '$s2'", repo.matchesCourse(s1, s2))
            }
        }
    }
}
