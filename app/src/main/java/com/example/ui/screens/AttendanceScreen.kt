package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseAttendance
import com.example.data.model.MANDATORY_ATTENDANCE_THRESHOLD
import com.example.data.model.OverallAttendance
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.DarkTextTertiary
import com.example.ui.theme.LocalDayProfile
import java.util.Locale

private val EmeraldGreen = Color(0xFF10B981)
private val WarningAmber = Color(0xFFF59E0B)
private val AlertRed = Color(0xFFEF4444)

/**
 * Term II Attendance Screen:
 * - Offline-first Room Database persistent attendance tracker.
 * - Displays all Term II subjects with individual percentage, safe zone status vs 80% threshold.
 * - Fast 1-tap manual entry for Present and Absent.
 * - Completely free of external LMS portal dependency.
 */
@Composable
fun AttendanceScreen(
    attendance: OverallAttendance,
    isSyncing: Boolean = false,
    onTriggerSync: () -> Unit = {},
    onLmsDataExtracted: (String, String?) -> Boolean = { _, _ -> false },
    onResetToOfficial: () -> Unit = {},
    onUpdateSubject: (String, Int, Int) -> Unit = { _, _, _ -> },
    onRecordAttendance: (String, Boolean) -> Unit = { _, _ -> },
    onAddNewCourse: (String, String, Int, Int, Int) -> Unit = { _, _, _, _, _ -> },
    onDeleteCourse: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dayProfile = LocalDayProfile.current

    var editingCourse by remember { mutableStateOf<CourseAttendance?>(null) }
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, SAFE, AT_RISK, CRITICAL

    val safeCount = attendance.courses.count { it.totalConductedClasses > 0 && it.percentage >= 80f && it.safeBunksRemaining > 0 }
    val warningCount = attendance.courses.count { it.totalConductedClasses > 0 && it.percentage >= 80f && it.safeBunksRemaining == 0 }
    val criticalCount = attendance.courses.count { it.totalConductedClasses > 0 && it.percentage < 80f }

    val filteredCourses = remember(attendance.courses, searchQuery, selectedFilter) {
        attendance.courses.filter { course ->
            val matchesFilter = when (selectedFilter) {
                "SAFE" -> course.totalConductedClasses > 0 && course.percentage >= 80f && course.safeBunksRemaining > 0
                "AT_RISK" -> course.totalConductedClasses > 0 && course.percentage >= 80f && course.safeBunksRemaining == 0
                "CRITICAL" -> course.totalConductedClasses > 0 && course.percentage < 80f
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                course.courseName.contains(searchQuery, ignoreCase = true) ||
                course.facultyName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Overall Term II Hero KPI Card
                OverallAttendanceHeroCard(
                    attendance = attendance,
                    onResetToOfficial = onResetToOfficial
                )
            }

            // Search Bar & Filter Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("attendance_search_field"),
                        placeholder = { Text("Search Term II courses or professors...", fontSize = 12.sp, color = DarkTextTertiary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = DarkTextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = DarkTextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedBorderColor = dayProfile.primaryAccent,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        singleLine = true
                    )

                    // Controls row: Course count, Filter chips & Add button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Term II Courses (${attendance.courses.size})",
                                    color = DarkTextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(dayProfile.primaryAccent.copy(alpha = 0.15f))
                                        .border(0.5.dp, dayProfile.primaryAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Safe: ≥ 80%",
                                        color = dayProfile.primaryAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Tap Present or Absent to log • Persistent in Room DB",
                                color = DarkTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { showAddCourseDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = dayProfile.primaryAccent),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_add_course")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            TextButton(
                                onClick = onResetToOfficial,
                                modifier = Modifier.testTag("restore_all_subjects_button"),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset",
                                    tint = dayProfile.primaryAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Reset", color = dayProfile.primaryAccent, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Filter Chips Bar
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        AttendanceFilterChip(
                            label = "All (${attendance.courses.size})",
                            isSelected = selectedFilter == "ALL",
                            color = dayProfile.primaryAccent,
                            onClick = { selectedFilter = "ALL" }
                        )
                    }
                    item {
                        AttendanceFilterChip(
                            label = "Safe Zone ($safeCount)",
                            isSelected = selectedFilter == "SAFE",
                            color = EmeraldGreen,
                            onClick = { selectedFilter = "SAFE" }
                        )
                    }
                    item {
                        AttendanceFilterChip(
                            label = "At Risk ($warningCount)",
                            isSelected = selectedFilter == "AT_RISK",
                            color = WarningAmber,
                            onClick = { selectedFilter = "AT_RISK" }
                        )
                    }
                    item {
                        AttendanceFilterChip(
                            label = "Shortage ($criticalCount)",
                            isSelected = selectedFilter == "CRITICAL",
                            color = AlertRed,
                            onClick = { selectedFilter = "CRITICAL" }
                        )
                    }
                }
            }

            // List of Term II Course Cards
            items(filteredCourses, key = { it.courseName }) { course ->
                TermIICourseAttendanceCard(
                    course = course,
                    onEdit = { editingCourse = course },
                    onRecordPresent = { onRecordAttendance(course.courseName, true) },
                    onRecordAbsent = { onRecordAttendance(course.courseName, false) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Add Course Dialog
    if (showAddCourseDialog) {
        AddCourseDialog(
            onDismiss = { showAddCourseDialog = false },
            onAdd = { name, fac, att, cond, sess ->
                onAddNewCourse(name, fac, att, cond, sess)
                showAddCourseDialog = false
            }
        )
    }

    // Quick review / verify dialog for a single course
    editingCourse?.let { course ->
        CourseAttendanceEditDialog(
            course = course,
            onDismiss = { editingCourse = null },
            onSave = { att, cond ->
                onUpdateSubject(course.courseName, att, cond)
                editingCourse = null
            },
            onDelete = {
                onDeleteCourse(course.courseName)
                editingCourse = null
            }
        )
    }
}

@Composable
private fun AttendanceFilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else DarkSurface)
            .border(
                1.dp,
                if (isSelected) color else DarkSurfaceBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) color else DarkTextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun OverallAttendanceHeroCard(
    attendance: OverallAttendance,
    onResetToOfficial: () -> Unit
) {
    val dayProfile = LocalDayProfile.current
    val percentage = attendance.overallPercentage
    val hasConducted = attendance.totalConducted > 0

    val statusColor = when {
        !hasConducted -> DarkTextTertiary
        percentage >= 80f -> EmeraldGreen
        percentage >= 75f -> WarningAmber
        else -> AlertRed
    }

    val statusText = when {
        !hasConducted -> "Term II Initialized • Awaiting First Lecture"
        percentage >= 85f -> "Excellent Attendance (Safe Zone)"
        percentage >= 80f -> "Safe • Above 80% Mandatory Benchmark"
        percentage >= 75f -> "Warning: Close to 80% Threshold"
        else -> "Attendance Shortage (< 80%)"
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (hasConducted) (percentage / 100f).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "hero_attendance_progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_attendance_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dayProfile.primaryAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MBA 2026-28 • TERM II",
                            color = dayProfile.primaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Aggregated across ${attendance.courses.size} Term II courses",
                        color = DarkTextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Safe zone badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (hasConducted) statusColor.copy(alpha = 0.15f) else DarkSurfaceElevated)
                        .border(1.dp, if (hasConducted) statusColor.copy(alpha = 0.4f) else DarkSurfaceBorder, RoundedCornerShape(50.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (percentage >= 80f) "SAFE ZONE" else "SHORTAGE",
                        color = if (hasConducted) statusColor else DarkTextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Percentage and Benchmark Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (hasConducted) String.format(Locale.getDefault(), "%.1f", percentage) else "100.0",
                            color = DarkTextPrimary,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 46.sp
                        )
                        Text(
                            text = "%",
                            color = dayProfile.primaryAccent,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (percentage >= 80f) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Minimum threshold reference
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Safe Zone Threshold",
                        color = DarkTextTertiary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "80.0%",
                        color = dayProfile.primaryAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mandatory Requirement",
                        color = DarkTextTertiary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar with 80% Benchmark
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = DarkSurfaceElevated,
                    strokeCap = StrokeCap.Round
                )

                // 80% Target Line Marker
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .height(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(2.dp)
                            .height(14.dp)
                            .background(dayProfile.primaryAccent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4 Stats KPI Pill Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiPill(
                    label = "Attended",
                    value = "${attendance.totalAttended}",
                    color = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
                KpiPill(
                    label = "Conducted",
                    value = "${attendance.totalConducted}",
                    color = dayProfile.primaryAccent,
                    modifier = Modifier.weight(1f)
                )
                KpiPill(
                    label = "Missed",
                    value = "${attendance.totalMissed}",
                    color = if (attendance.totalMissed > 0) AlertRed else DarkTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                KpiPill(
                    label = "Safe Bunks",
                    value = "${attendance.safeBunksRemaining}",
                    color = if (attendance.safeBunksRemaining > 0) EmeraldGreen else WarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contextual Guidance Note
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceElevated)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (percentage >= 80f) {
                        "You can safely miss ${attendance.safeBunksRemaining} upcoming lecture(s) across courses while maintaining ≥ 80%."
                    } else {
                        "Must attend next ${attendance.classesNeededFor80} consecutive class(es) to restore attendance above 80%."
                    },
                    color = DarkTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun KpiPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .border(0.5.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = color,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = DarkTextTertiary,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Course Card for each Term II Subject with its own percentage,
 * 80% Safe Zone indicators, and manual Present/Absent entry buttons.
 */
@Composable
private fun TermIICourseAttendanceCard(
    course: CourseAttendance,
    onEdit: () -> Unit,
    onRecordPresent: () -> Unit,
    onRecordAbsent: () -> Unit
) {
    val dayProfile = LocalDayProfile.current
    val percentage = course.percentage
    val isNotStarted = course.isNotStarted
    val thresholdDelta = percentage - MANDATORY_ATTENDANCE_THRESHOLD

    val isSafe = percentage >= MANDATORY_ATTENDANCE_THRESHOLD && !isNotStarted
    val isAtRisk = isSafe && course.safeBunksRemaining == 0
    val isCritical = !isNotStarted && percentage < MANDATORY_ATTENDANCE_THRESHOLD

    val statusColor = when {
        isNotStarted -> DarkTextTertiary
        isCritical -> AlertRed
        isAtRisk -> WarningAmber
        else -> EmeraldGreen
    }

    val nextAttendedPct = if (course.totalConductedClasses > 0) {
        ((course.attendedClasses + 1).toFloat() / (course.totalConductedClasses + 1)) * 100f
    } else 100f
    val nextMissedPct = if (course.totalConductedClasses > 0) {
        (course.attendedClasses.toFloat() / (course.totalConductedClasses + 1)) * 100f
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("course_card_${course.courseName.replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCritical) AlertRed.copy(alpha = 0.4f) else DarkSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Subject Name, Faculty Name, and Percentage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.courseName,
                        color = DarkTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (course.facultyName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = course.facultyName,
                            color = DarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Percentage Badge: Each subject prominently shows its own %
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isNotStarted) DarkSurfaceElevated else statusColor.copy(alpha = 0.15f))
                            .border(0.5.dp, if (isNotStarted) DarkSurfaceBorder else statusColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isNotStarted) "—" else String.format(Locale.getDefault(), "%.1f%%", percentage),
                            color = if (isNotStarted) DarkTextSecondary else statusColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = when {
                            isNotStarted -> "0 Conducted"
                            thresholdDelta >= 0 -> String.format(Locale.getDefault(), "+%.1f%% safe", thresholdDelta)
                            else -> String.format(Locale.getDefault(), "%.1f%% deficit", thresholdDelta)
                        },
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar with 80% Safe Zone target marker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (!isNotStarted) {
                    LinearProgressIndicator(
                        progress = { (percentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = statusColor,
                        trackColor = DarkSurfaceElevated,
                        strokeCap = StrokeCap.Round
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(DarkSurfaceBorder.copy(alpha = 0.4f))
                    )
                }

                // 80% Target Line Marker
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .height(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(2.dp)
                            .height(14.dp)
                            .background(dayProfile.primaryAccent)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "0%", color = DarkTextTertiary, fontSize = 9.sp)
                Text(text = "Target: 80% Safe Zone", color = dayProfile.primaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(text = "100%", color = DarkTextTertiary, fontSize = 9.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attendance Count & Status Guidance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusMessage = when {
                    isNotStarted -> "No sessions conducted yet"
                    isCritical -> "Deficit: Attend next ${course.classesNeededFor80} class(es) for 80%"
                    isAtRisk -> "0 bunks left! Next absence drops below 80%"
                    else -> "Safe: ${course.safeBunksRemaining} bunk(s) allowed above 80%"
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            isNotStarted -> Icons.Default.Info
                            isCritical -> Icons.Default.Warning
                            isAtRisk -> Icons.Default.Warning
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${course.attendedClasses}/${course.totalConductedClasses} Attended • $statusMessage",
                        color = if (isCritical || isAtRisk) statusColor else DarkTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isCritical || isAtRisk) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Attendance",
                        tint = DarkTextTertiary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // MANUAL ENTRY: Fast Present and Absent Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRecordPresent,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_present_${course.courseName.replace(" ", "_")}"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Present (+1)",
                        color = EmeraldGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onRecordAbsent,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_absent_${course.courseName.replace(" ", "_")}"),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = AlertRed,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Absent (+1)",
                        color = AlertRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isNotStarted) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "If Present next: %.1f%%", nextAttendedPct),
                        color = DarkTextTertiary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "If Absent next: %.1f%%", nextMissedPct),
                        color = DarkTextTertiary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Manual edit dialog to fine-tune attended/conducted counts or remove course.
 */
@Composable
private fun CourseAttendanceEditDialog(
    course: CourseAttendance,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit,
    onDelete: () -> Unit = {}
) {
    val dayProfile = LocalDayProfile.current
    var attendedText by remember { mutableStateOf(course.attendedClasses.toString()) }
    var conductedText by remember { mutableStateOf(course.totalConductedClasses.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Column {
                Text(
                    text = course.courseName,
                    color = DarkTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manual Attendance Adjustment",
                    color = dayProfile.primaryAccent,
                    fontSize = 11.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Update the exact session numbers for this course:",
                    color = DarkTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = attendedText,
                    onValueChange = { attendedText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Attended Classes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = dayProfile.primaryAccent,
                        unfocusedBorderColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = conductedText,
                    onValueChange = { conductedText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Total Conducted Classes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = dayProfile.primaryAccent,
                        unfocusedBorderColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Subject", fontSize = 12.sp, color = AlertRed)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val att = attendedText.toIntOrNull() ?: course.attendedClasses
                    val cond = conductedText.toIntOrNull() ?: course.totalConductedClasses
                    onSave(att, cond)
                },
                colors = ButtonDefaults.buttonColors(containerColor = dayProfile.primaryAccent)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DarkTextSecondary)
            }
        }
    )
}

/**
 * Dialog to add an extra course/elective to the Room database.
 */
@Composable
private fun AddCourseDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, faculty: String, attended: Int, conducted: Int, totalSessions: Int) -> Unit
) {
    val dayProfile = LocalDayProfile.current
    var courseName by remember { mutableStateOf("") }
    var facultyName by remember { mutableStateOf("") }
    var attendedText by remember { mutableStateOf("0") }
    var conductedText by remember { mutableStateOf("0") }
    var totalSessionsText by remember { mutableStateOf("20") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Column {
                Text(
                    text = "Add Term II Course",
                    color = DarkTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Track attendance with 80% Safe Zone requirement",
                    color = dayProfile.primaryAccent,
                    fontSize = 11.sp
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("Course Name *") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = dayProfile.primaryAccent,
                        unfocusedBorderColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = facultyName,
                    onValueChange = { facultyName = it },
                    label = { Text("Faculty / Professor Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = dayProfile.primaryAccent,
                        unfocusedBorderColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = attendedText,
                        onValueChange = { attendedText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Attended") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedBorderColor = dayProfile.primaryAccent,
                            unfocusedBorderColor = DarkSurfaceBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = conductedText,
                        onValueChange = { conductedText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Conducted") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary,
                            focusedBorderColor = dayProfile.primaryAccent,
                            unfocusedBorderColor = DarkSurfaceBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = totalSessionsText,
                    onValueChange = { totalSessionsText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Total Term Sessions") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary,
                        focusedBorderColor = dayProfile.primaryAccent,
                        unfocusedBorderColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (courseName.isNotBlank()) {
                        val att = attendedText.toIntOrNull() ?: 0
                        val cond = conductedText.toIntOrNull() ?: 0
                        val sess = totalSessionsText.toIntOrNull() ?: 20
                        onAdd(courseName.trim(), facultyName.trim(), att, cond, sess)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = dayProfile.primaryAccent),
                enabled = courseName.isNotBlank()
            ) {
                Text("Add Course", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DarkTextSecondary)
            }
        }
    )
}
