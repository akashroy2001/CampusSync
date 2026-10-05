package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AttendanceLogEntity
import com.example.data.local.entity.CourseAttendanceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room database operations on course attendance.
 */
@Dao
interface AttendanceDao {

    @Query("SELECT * FROM course_attendance ORDER BY courseName ASC")
    fun getAllCoursesFlow(): Flow<List<CourseAttendanceEntity>>

    @Query("SELECT * FROM course_attendance ORDER BY courseName ASC")
    suspend fun getAllCourses(): List<CourseAttendanceEntity>

    @Query("SELECT * FROM course_attendance WHERE id = :id")
    fun getCourseByIdFlow(id: Long): Flow<CourseAttendanceEntity?>

    @Query("SELECT * FROM course_attendance WHERE id = :id")
    suspend fun getCourseById(id: Long): CourseAttendanceEntity?

    @Query("SELECT * FROM course_attendance WHERE courseName = :name LIMIT 1")
    suspend fun getCourseByName(name: String): CourseAttendanceEntity?

    @Query("SELECT COUNT(*) FROM course_attendance")
    suspend fun getCourseCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseAttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseAttendanceEntity>)

    @Update
    suspend fun updateCourse(course: CourseAttendanceEntity)

    @Delete
    suspend fun deleteCourse(course: CourseAttendanceEntity)

    @Query("DELETE FROM course_attendance WHERE id = :id")
    suspend fun deleteCourseById(id: Long)

    @Query("DELETE FROM course_attendance")
    suspend fun deleteAllCourses()

    // Helper updates
    @Query("""
        UPDATE course_attendance 
        SET attendedClasses = :attended, totalConductedClasses = :conducted, lastUpdatedTimestamp = :timestamp 
        WHERE id = :id
    """)
    suspend fun updateAttendanceCounts(id: Long, attended: Int, conducted: Int, timestamp: Long = System.currentTimeMillis())

    // Log operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AttendanceLogEntity): Long

    @Query("SELECT * FROM attendance_logs WHERE courseId = :courseId ORDER BY timestamp DESC")
    fun getLogsForCourseFlow(courseId: Long): Flow<List<AttendanceLogEntity>>

    @Query("SELECT * FROM attendance_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogsFlow(): Flow<List<AttendanceLogEntity>>

    @Query("DELETE FROM attendance_logs WHERE courseId = :courseId")
    suspend fun deleteLogsForCourse(courseId: Long)
}
