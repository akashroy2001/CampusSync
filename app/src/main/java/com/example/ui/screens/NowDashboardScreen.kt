package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MealStatus
import com.example.data.model.SlotStatus
import com.example.data.repository.MessMenuRepository
import com.example.data.repository.ScheduleRepository
import com.example.ui.components.ClassStatusBadge
import com.example.ui.components.MealStatusBadge
import com.example.ui.components.VegNonVegIndicator
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LocalDayProfile
import com.example.ui.theme.UpNextAmber
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun NowDashboardScreen(
    selectedDate: LocalDate,
    activeClassInfo: ScheduleRepository.ActiveClassInfo?,
    currentMealStatus: MessMenuRepository.CurrentMealStatus,
    onNavigateToSchedule: () -> Unit,
    onNavigateToMess: () -> Unit,
    onNavigateToAttendance: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dayProfile = LocalDayProfile.current
    val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Hero Card with Dynamic Day Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("now_dashboard_hero")
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            dayProfile.primaryAccent,
                            dayProfile.primaryAccent.copy(alpha = 0.75f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TODAY ON CAMPUS",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = formattedDate,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "TERM II",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mini summary banner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (activeClassInfo?.isClassLiveNow == true) {
                            "Live lecture currently in session"
                        } else {
                            activeClassInfo?.statusText ?: "MBA Term II Schedule Synced"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 1: CLASS QUICK-GLANCE WIDGET
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = dayProfile.primaryAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lecture Status",
                    color = DarkTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Full Timetable →",
                color = dayProfile.primaryAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .testTag("now_view_all_schedule_button")
                    .clickable { onNavigateToSchedule() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val relevantClass = activeClassInfo?.activeClass ?: activeClassInfo?.nextClass
        val isHoliday = activeClassInfo?.daySchedule?.isHoliday == true
        val classStripeColor = when {
            activeClassInfo?.isClassLiveNow == true -> dayProfile.primaryAccent
            relevantClass?.status == SlotStatus.UP_NEXT -> UpNextAmber
            else -> DarkSurfaceBorder
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("now_class_widget_card")
                .clip(RoundedCornerShape(16.dp))
                .drawBehind {
                    drawRect(
                        color = classStripeColor,
                        topLeft = Offset.Zero,
                        size = Size(width = 4.dp.toPx(), height = size.height)
                    )
                },
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)) {
                if (isHoliday) {
                    // Holiday card
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x338B5CF6))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = activeClassInfo?.daySchedule?.holidayTitle?.ifBlank { "Holiday" } ?: "Holiday",
                                color = DarkTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Institution Holiday / No Regular Classes Scheduled",
                                color = DarkTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else if (relevantClass != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Slot ${relevantClass.slot.slotNumber} • ${relevantClass.slot.timeRange}",
                            color = DarkTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        ClassStatusBadge(status = relevantClass.status)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = relevantClass.courseName.ifBlank { "Class Lecture" },
                        color = if (activeClassInfo?.isClassLiveNow == true) Color.White else DarkTextPrimary,
                        fontSize = if (activeClassInfo?.isClassLiveNow == true) 18.sp else 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (relevantClass.sessionNumber.isNotBlank()) {
                        Text(
                            text = "Session: ${relevantClass.sessionNumber}",
                            color = dayProfile.primaryAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (relevantClass.facultyName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = relevantClass.facultyName,
                            color = DarkTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Active progress bar if live now
                    if (activeClassInfo?.isClassLiveNow == true) {
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

                        // Next Upcoming Class (or Tomorrow's class if this is the final class today)
                        val upcomingToday = activeClassInfo.nextClass
                        if (upcomingToday != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = UpNextAmber,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "NEXT CLASS • Slot ${upcomingToday.slot.slotNumber}",
                                                color = UpNextAmber,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }

                                        val timeDesc = if (upcomingToday.minutesUntilStart > 0) {
                                            "${upcomingToday.slot.formattedStartTime} (in ${upcomingToday.minutesUntilStart}m)"
                                        } else {
                                            upcomingToday.slot.timeRange
                                        }
                                        Text(
                                            text = timeDesc,
                                            color = DarkTextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = upcomingToday.courseName.ifBlank { "Upcoming Class" },
                                        color = DarkTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    val nextMeta = listOf(
                                        if (upcomingToday.sessionNumber.isNotBlank()) "Session ${upcomingToday.sessionNumber}" else null,
                                        if (upcomingToday.facultyName.isNotBlank()) upcomingToday.facultyName else null
                                    ).filterNotNull().joinToString(" • ")

                                    if (nextMeta.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = nextMeta,
                                            color = DarkTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        } else if (activeClassInfo.tomorrowUpcomingClass != null) {
                            val tom = activeClassInfo.tomorrowUpcomingClass
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = dayProfile.primaryAccent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "NEXT: ${activeClassInfo.tomorrowDateText?.uppercase() ?: "TOMORROW"}",
                                                color = dayProfile.primaryAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }

                                        Text(
                                            text = "Slot ${tom.slot.slotNumber} • ${tom.slot.formattedStartTime}",
                                            color = DarkTextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = tom.courseName,
                                        color = DarkTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // No more classes today -> Show celebration and tomorrow's upcoming lecture
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = dayProfile.primaryAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "No More Lectures Today",
                                    color = DarkTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "All scheduled classes have concluded.",
                                    color = DarkTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Tomorrow's upcoming lecture
                        if (activeClassInfo?.tomorrowUpcomingClass != null) {
                            val tomClass = activeClassInfo.tomorrowUpcomingClass
                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(dayProfile.primaryAccent.copy(alpha = 0.08f))
                                    .border(1.dp, dayProfile.primaryAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(dayProfile.primaryAccent.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "NEXT: ${activeClassInfo.tomorrowDateText?.uppercase() ?: "TOMORROW"}",
                                                color = dayProfile.primaryAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }

                                        Text(
                                            text = "Slot ${tomClass.slot.slotNumber} • ${tomClass.slot.timeRange}",
                                            color = DarkTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = tomClass.courseName.ifBlank { "Scheduled Class" },
                                        color = DarkTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    val meta = listOf(
                                        if (tomClass.sessionNumber.isNotBlank()) "Session ${tomClass.sessionNumber}" else null,
                                        if (tomClass.facultyName.isNotBlank()) tomClass.facultyName else null
                                    ).filterNotNull().joinToString(" • ")

                                    if (meta.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = meta,
                                            color = DarkTextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: MESS QUICK-GLANCE WIDGET
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = dayProfile.primaryAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mess Dining",
                    color = DarkTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Full Menu →",
                color = dayProfile.primaryAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .testTag("now_view_full_mess_button")
                    .clickable { onNavigateToMess() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Countdown Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mess_countdown_banner")
                .clip(RoundedCornerShape(16.dp))
                .background(dayProfile.primaryAccent.copy(alpha = 0.10f))
                .border(1.dp, dayProfile.primaryAccent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(dayProfile.primaryAccent.copy(alpha = 0.20f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = dayProfile.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (currentMealStatus.isAllTodayMealsEnded) "TODAY'S MEALS COMPLETED" else "MEAL SCHEDULE STATUS",
                        color = dayProfile.primaryAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currentMealStatus.bannerText,
                        color = DarkTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val displayedMeal = currentMealStatus.displayedMeal

        if (displayedMeal != null) {
            val mealStripeColor = when {
                currentMealStatus.isMealActiveNow -> com.example.ui.theme.LiveGreen
                else -> UpNextAmber
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("now_meal_widget_card")
                    .clip(RoundedCornerShape(16.dp))
                    .drawBehind {
                        drawRect(
                            color = mealStripeColor,
                            topLeft = Offset.Zero,
                            size = Size(width = 4.dp.toPx(), height = size.height)
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val mealTitle = if (currentMealStatus.isAllTodayMealsEnded) {
                                "Tomorrow's Breakfast"
                            } else {
                                displayedMeal.mealType.displayName
                            }
                            Text(
                                text = mealTitle,
                                color = DarkTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${displayedMeal.mealType.timeWindow} • ${currentMealStatus.displayedMealDayName}",
                                color = DarkTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        val badgeStatus = when {
                            currentMealStatus.isMealActiveNow -> MealStatus.SERVING_NOW
                            else -> MealStatus.UP_NEXT
                        }
                        MealStatusBadge(status = badgeStatus)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dishes with veg / non-veg indicators
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        displayedMeal.dishes.take(5).forEach { dish ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                VegNonVegIndicator(isNonVeg = dish.isNonVeg)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = dish.name,
                                    color = if (dish.isNonVeg) Color(0xFFFCA5A5) else DarkTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (dish.isNonVeg) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (dish.isNonVeg) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(Non-Veg)",
                                        color = Color(0xFFEF4444),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (displayedMeal.dishes.size > 5) {
                            Text(
                                text = "+ ${displayedMeal.dishes.size - 5} more items in full menu",
                                color = DarkTextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Link Card: Term I Overview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSchedule() },
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(dayProfile.primaryAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = dayProfile.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MBA Term I Schedule",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkTextPrimary
                        )
                        Text(
                            text = "Tap to view full term calendar & all lectures",
                            fontSize = 12.sp,
                            color = DarkTextSecondary
                        )
                    }
                }

                Text(
                    text = "Open →",
                    color = dayProfile.primaryAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (onNavigateToAttendance != null) {
            Spacer(modifier = Modifier.height(12.dp))

            // Quick Link Card: Attendance & LMS
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAttendance() },
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(dayProfile.primaryAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentTurnedIn,
                                contentDescription = null,
                                tint = dayProfile.primaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Attendance & LMS Portal",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkTextPrimary
                            )
                            Text(
                                text = "80% mandatory threshold • Auto-synced from LMS",
                                fontSize = 12.sp,
                                color = DarkTextSecondary
                            )
                        }
                    }

                    Text(
                        text = "Check →",
                        color = dayProfile.primaryAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
