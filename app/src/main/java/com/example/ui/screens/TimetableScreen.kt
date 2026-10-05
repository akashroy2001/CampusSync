package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DaySchedule
import com.example.data.model.ScheduleClass
import com.example.data.model.SlotStatus
import com.example.ui.components.ClassStatusBadge
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.DarkTextTertiary
import com.example.ui.theme.HolidayPurple
import com.example.ui.theme.LocalDayProfile
import com.example.ui.theme.UpNextAmber
import com.example.ui.viewmodel.ScheduleViewMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TimetableScreen(
    selectedDate: LocalDate,
    viewMode: ScheduleViewMode,
    daySchedule: DaySchedule?,
    weekSchedules: List<DaySchedule>,
    isLoading: Boolean,
    searchQuery: String,
    onViewModeChange: (ScheduleViewMode) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onJumpToToday: () -> Unit,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayProfile = LocalDayProfile.current
    val today = LocalDate.now()
    val isTodaySelected = selectedDate == today

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search Bar for Courses or Professors
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search course or professor...", color = DarkTextTertiary, fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = DarkTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = DarkTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceCard,
                unfocusedContainerColor = DarkSurfaceCard,
                focusedBorderColor = dayProfile.primaryAccent,
                unfocusedBorderColor = DarkSurfaceBorder,
                focusedTextColor = DarkTextPrimary,
                unfocusedTextColor = DarkTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("schedule_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Segmented View Toggle: Sleek pill navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50.dp))
                .background(DarkSurfaceCard)
                .padding(4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (viewMode == ScheduleViewMode.TODAYS_AGENDA) dayProfile.primaryAccent else Color.Transparent)
                    .clickable { onViewModeChange(ScheduleViewMode.TODAYS_AGENDA) }
                    .padding(vertical = 8.dp)
                    .testTag("tab_todays_agenda")
            ) {
                Text(
                    text = "Daily Agenda",
                    fontWeight = if (viewMode == ScheduleViewMode.TODAYS_AGENDA) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (viewMode == ScheduleViewMode.TODAYS_AGENDA) Color.White else DarkTextSecondary,
                    fontSize = 13.sp
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (viewMode == ScheduleViewMode.FULL_WEEK) dayProfile.primaryAccent else Color.Transparent)
                    .clickable { onViewModeChange(ScheduleViewMode.FULL_WEEK) }
                    .padding(vertical = 8.dp)
                    .testTag("tab_full_week")
            ) {
                Text(
                    text = "Full Term View",
                    fontWeight = if (viewMode == ScheduleViewMode.FULL_WEEK) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (viewMode == ScheduleViewMode.FULL_WEEK) Color.White else DarkTextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Day Chips row for rapid navigation across the term schedule
        val dateScrollState = rememberScrollState()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Term Dates",
                color = DarkTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (!isTodaySelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(dayProfile.primaryAccent.copy(alpha = 0.15f))
                        .clickable { onJumpToToday() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        tint = dayProfile.primaryAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Jump to Today",
                        color = dayProfile.primaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(dateScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            weekSchedules.forEach { sched ->
                val parsedDate = try {
                    LocalDate.parse(sched.dateStr, dtf)
                } catch (e: Exception) {
                    null
                }
                val isSelected = (parsedDate != null && parsedDate == selectedDate) ||
                        (parsedDate == null && sched.dayName.equals(selectedDate.dayOfWeek.name, ignoreCase = true))
                val isTodayChip = parsedDate != null && parsedDate == today

                Box(
                    modifier = Modifier
                        .testTag("date_chip_${sched.dateStr}")
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) dayProfile.primaryAccent else DarkSurfaceCard)
                        .border(
                            1.dp,
                            if (isSelected) dayProfile.primaryAccent else if (isTodayChip) dayProfile.primaryAccent.copy(alpha = 0.5f) else DarkSurfaceBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            if (parsedDate != null) onDateSelect(parsedDate)
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sched.dayName.take(3).uppercase(),
                                color = if (isSelected) Color.White else DarkTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isTodayChip) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else dayProfile.primaryAccent)
                                )
                            }
                        }
                        Text(
                            text = sched.dateStr.take(5),
                            color = if (isSelected) Color.White else DarkTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator(color = dayProfile.primaryAccent)
            }
        } else if (viewMode == ScheduleViewMode.TODAYS_AGENDA) {
            // Today's Agenda View
            if (daySchedule == null) {
                EmptyScheduleView()
            } else if (daySchedule.isHoliday) {
                HolidayBannerCard(holidayTitle = daySchedule.holidayTitle, dateStr = daySchedule.dateStr)
            } else {
                val nonFreeClasses = daySchedule.classes.filter { !it.isFreePeriod && !it.isHoliday }
                val isNoLecturesDay = nonFreeClasses.isEmpty()

                val filteredClasses = if (searchQuery.isBlank()) {
                    daySchedule.classes
                } else {
                    daySchedule.classes.filter {
                        it.courseName.contains(searchQuery, ignoreCase = true) ||
                        it.facultyName.contains(searchQuery, ignoreCase = true) ||
                        it.holidayName.contains(searchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("schedule_class_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${daySchedule.dayName}, ${daySchedule.dateStr}",
                                color = DarkTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isTodaySelected) "TODAY • 5 Class Slots" else "5 Class Slots",
                                color = dayProfile.primaryAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isNoLecturesDay && searchQuery.isBlank()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EventNote,
                                        contentDescription = null,
                                        tint = dayProfile.primaryAccent,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No Lectures Scheduled",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "All slots for ${daySchedule.dayName} are free periods. Great time for self-study and assignments!",
                                        fontSize = 12.sp,
                                        color = DarkTextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredClasses) { classItem ->
                            ClassSlotCard(
                                classItem = classItem,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        } else {
            // Full Term Schedule View
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("full_week_schedule_list"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(weekSchedules) { day ->
                    val dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                    val parsedDate = try { LocalDate.parse(day.dateStr, dtf) } catch (e: Exception) { null }
                    val isDayToday = parsedDate != null && parsedDate == today

                    Column {
                        // Day Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isDayToday) dayProfile.primaryAccent else DarkTextSecondary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${day.dayName} (${day.dateStr})",
                                color = if (isDayToday) dayProfile.primaryAccent else DarkTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isDayToday) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(dayProfile.primaryAccent.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "TODAY",
                                        color = dayProfile.primaryAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (day.isHoliday) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x338B5CF6))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "CAMPUS EVENT / HOLIDAY",
                                        color = Color(0xFFC4B5FD),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (day.isHoliday) {
                            HolidayBannerCard(holidayTitle = day.holidayTitle, dateStr = day.dateStr)
                        } else {
                            val filtered = if (searchQuery.isBlank()) {
                                day.classes
                            } else {
                                day.classes.filter {
                                    it.courseName.contains(searchQuery, ignoreCase = true) ||
                                    it.facultyName.contains(searchQuery, ignoreCase = true)
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filtered.forEach { classItem ->
                                    ClassSlotCard(
                                        classItem = classItem,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
fun ClassSlotCard(
    classItem: ScheduleClass,
    modifier: Modifier = Modifier
) {
    val dayProfile = LocalDayProfile.current
    val isLive = classItem.status == SlotStatus.LIVE_NOW
    val isCompleted = classItem.status == SlotStatus.COMPLETED
    val isFree = classItem.isFreePeriod

    val stripeColor = when {
        isLive -> dayProfile.primaryAccent
        classItem.status == SlotStatus.UP_NEXT -> UpNextAmber
        isCompleted -> DarkSurfaceBorder
        else -> DarkSurfaceBorder
    }

    Card(
        modifier = modifier
            .testTag("class_slot_card_${classItem.slot.slotNumber}")
            .alpha(if (isCompleted) 0.6f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRect(
                    color = stripeColor,
                    topLeft = Offset.Zero,
                    size = Size(width = 4.dp.toPx(), height = size.height)
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) DarkSurfaceCard.copy(alpha = 0.5f) else DarkSurfaceCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
        ) {
            // Header: Slot + Time Range on left, Status Badge on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Slot ${classItem.slot.slotNumber} • ${classItem.slot.timeRange}",
                    color = if (isCompleted) DarkTextSecondary else DarkTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                ClassStatusBadge(status = classItem.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body: Course title and session number
            if (isFree) {
                Text(
                    text = "Free Period / Self-Study",
                    color = DarkTextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = classItem.courseName,
                        color = if (isLive) Color.White else DarkTextPrimary,
                        fontSize = if (isLive) 17.sp else 16.sp,
                        fontWeight = if (isLive) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    if (classItem.sessionNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceBorder)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "S${classItem.sessionNumber}",
                                color = DarkTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Row 2: Faculty Name
                if (classItem.facultyName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = classItem.facultyName,
                        color = DarkTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Live Now Active Progress Bar
                if (isLive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(dayProfile.primaryAccent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HolidayBannerCard(
    holidayTitle: String,
    dateStr: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("holiday_banner_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0x228B5CF6)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, HolidayPurple)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x338B5CF6))
            ) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    tint = Color(0xFFC4B5FD),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = holidayTitle.ifBlank { "Campus Holiday / No Lectures" },
                    color = Color(0xFFDDD6FE),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "No regular academic slots scheduled for $dateStr",
                    color = Color(0xFFA78BFA),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyScheduleView() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                tint = DarkTextTertiary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Schedule Found",
                color = DarkTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Timetable data may still be loading or parsing.",
                color = DarkTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
