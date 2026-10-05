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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
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
 * - Direct, clean subject attendance tracker.
 * - Synced with timetable schedule for conducted class counts.
 * - 1-tap options matching the conducted classes (e.g. 2/2 Present, 1/2 Present, 0/2 Present).
 * - Individual safe zone status per subject (80% benchmark).
 * - No aggregated summary section or external portal dependencies.
 */
@Composable
fun AttendanceScreen(
    attendance: OverallAttendance,
    onSetAttendedCount: (String, Int) -> Unit = { _, _ -> },
    onResetToOfficial: () -> Unit = {},
    onUpdateSubject: (String, Int, Int) -> Unit = { _, _, _ -> },
    onDeleteCourse: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dayProfile = LocalDayProfile.current

    var editingCourse by remember { mutableStateOf<CourseAttendance?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, SAFE, SHORTAGE

    val safeCount = attendance.courses.count { it.totalConductedClasses > 0 && it.percentage >= MANDATORY_ATTENDANCE_THRESHOLD }
    val shortageCount = attendance.courses.count { it.totalConductedClasses > 0 && it.percentage < MANDATORY_ATTENDANCE_THRESHOLD }

    val filteredCourses = remember(attendance.courses, searchQuery, selectedFilter) {
        attendance.courses.filter { course ->
            val matchesFilter = when (selectedFilter) {
                "SAFE" -> course.totalConductedClasses > 0 && course.percentage >= MANDATORY_ATTENDANCE_THRESHOLD
                "SHORTAGE" -> course.totalConductedClasses > 0 && course.percentage < MANDATORY_ATTENDANCE_THRESHOLD
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))

                // Clean Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Term II Attendance",
                                color = DarkTextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(dayProfile.primaryAccent.copy(alpha = 0.15f))
                                    .border(0.5.dp, dayProfile.primaryAccent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Safe: ≥ 80%",
                                    color = dayProfile.primaryAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Synced with schedule • Tap attendance option per course",
                            color = DarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onResetToOfficial,
                        modifier = Modifier.testTag("restore_all_subjects_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Attendance to Schedule",
                            tint = dayProfile.primaryAccent
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("attendance_search_field"),
                    placeholder = {
                        Text("Search Term II courses or professors...", fontSize = 13.sp, color = DarkTextTertiary)
                    },
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
                            label = "Shortage ($shortageCount)",
                            isSelected = selectedFilter == "SHORTAGE",
                            color = AlertRed,
                            onClick = { selectedFilter = "SHORTAGE" }
                        )
                    }
                }
            }

            // Clean list of Subject Cards
            items(filteredCourses, key = { it.courseName }) { course ->
                TermIICourseCard(
                    course = course,
                    onSelectAttended = { attended ->
                        onSetAttendedCount(course.courseName, attended)
                    },
                    onEdit = { editingCourse = course }
                )
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Manual edit dialog to fine-tune attended/conducted counts or remove course
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
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) color else DarkTextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Clean, spacious, uncluttered card for each Term II Course.
 * Shows:
 * 1. Course title & professor
 * 2. Individual % and SAFE ZONE / SHORTAGE indicator
 * 3. Schedule context: classes done so far
 * 4. Manual options synced to conducted count (e.g. 2/2 Present, 1/2 Present, 0/2 Present)
 */
@Composable
private fun TermIICourseCard(
    course: CourseAttendance,
    onSelectAttended: (Int) -> Unit,
    onEdit: () -> Unit
) {
    val dayProfile = LocalDayProfile.current
    val conducted = course.totalConductedClasses
    val attended = course.attendedClasses
    val percentage = course.percentage
    val isNotStarted = course.isNotStarted

    val isSafe = !isNotStarted && percentage >= MANDATORY_ATTENDANCE_THRESHOLD
    val statusColor = when {
        isNotStarted -> DarkTextTertiary
        isSafe -> EmeraldGreen
        else -> AlertRed
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (conducted > 0) (percentage / 100f).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("course_card_${course.courseName.replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (!isNotStarted && !isSafe) AlertRed.copy(alpha = 0.35f) else DarkSurfaceBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Subject Name, Faculty Name, and Percentage Badge
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
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp
                    )

                    if (course.facultyName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = course.facultyName,
                            color = DarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = DarkTextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                conducted == 0 -> "No classes held yet in schedule"
                                conducted == 1 -> "1 class done so far"
                                else -> "$conducted classes done so far"
                            },
                            color = DarkTextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Percentage & Zone Badge
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isNotStarted) DarkSurfaceElevated else statusColor.copy(alpha = 0.15f))
                            .border(
                                1.dp,
                                if (isNotStarted) DarkSurfaceBorder else statusColor.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isNotStarted) "—" else String.format(Locale.getDefault(), "%.1f%%", percentage),
                            color = if (isNotStarted) DarkTextSecondary else statusColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            isNotStarted -> "Pending"
                            isSafe -> "SAFE ZONE"
                            else -> "SHORTAGE"
                        },
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Progress Bar (when classes have been conducted)
            if (conducted > 0) {
                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = DarkSurfaceElevated,
                        strokeCap = StrokeCap.Round
                    )

                    // 80% Benchmark Line Marker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.80f)
                            .height(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(2.dp)
                                .height(12.dp)
                                .background(dayProfile.primaryAccent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Contextual Insight Line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isSafe) {
                                if (course.safeBunksRemaining > 0) {
                                    "Safe: Can miss ${course.safeBunksRemaining} upcoming lecture(s) above 80%"
                                } else {
                                    "On the brink: 0 bunks left to stay above 80%"
                                }
                            } else {
                                "Shortage: Attend next ${course.classesNeededFor80} class(es) to reach 80%"
                            },
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
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
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // MANUAL ATTENDANCE SELECTION
            // Synced to the current day and all previous classes of that subject
            Text(
                text = "Attendance Selection:",
                color = DarkTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (conducted == 0) {
                // When 0 classes conducted so far
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = DarkTextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "No classes conducted yet • Attendance options unlock once first lecture begins",
                            color = DarkTextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            } else if (conducted in 1..4) {
                // Show clean options for each possible attendance count
                // e.g. for conducted == 2: "2/2 Present (100%)", "1/2 Present (50%)", "0/2 Present (0%)"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (count in conducted downTo 0) {
                        val isSelected = attended == count
                        val optionPct = (count.toFloat() / conducted.toFloat()) * 100f
                        val optionSafe = optionPct >= MANDATORY_ATTENDANCE_THRESHOLD
                        val optionColor = if (optionSafe) EmeraldGreen else AlertRed

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) optionColor.copy(alpha = 0.18f) else DarkSurfaceElevated
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) optionColor else DarkSurfaceBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectAttended(count) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = optionColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                    }
                                    Text(
                                        text = "$count/$conducted Present",
                                        color = if (isSelected) DarkTextPrimary else DarkTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%.0f%%", optionPct),
                                    color = if (isSelected) optionColor else DarkTextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                // When conducted >= 5: Clean Stepper + Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$attended / $conducted Present (${String.format(Locale.getDefault(), "%.0f%%", percentage)})",
                        color = statusColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { if (attended > 0) onSelectAttended(attended - 1) },
                            enabled = attended > 0,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(1.dp, DarkSurfaceBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease",
                                tint = if (attended > 0) DarkTextPrimary else DarkTextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { if (attended < conducted) onSelectAttended(attended + 1) },
                            enabled = attended < conducted,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(1.dp, DarkSurfaceBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase",
                                tint = if (attended < conducted) dayProfile.primaryAccent else DarkTextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        conducted to "All Present",
                        (conducted - 1).coerceAtLeast(0) to "Missed 1",
                        (conducted - 2).coerceAtLeast(0) to "Missed 2"
                    )

                    presets.forEach { (cnt, label) ->
                        val isSelected = attended == cnt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) dayProfile.primaryAccent.copy(alpha = 0.2f) else DarkSurfaceElevated)
                                .border(1.dp, if (isSelected) dayProfile.primaryAccent else DarkSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable { onSelectAttended(cnt) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$label ($cnt/$conducted)",
                                color = if (isSelected) dayProfile.primaryAccent else DarkTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
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
                    fontSize = 12.sp
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
