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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.model.DayMessMenu
import com.example.data.model.MealMenu
import com.example.data.model.MealStatus
import com.example.data.repository.MessMenuRepository
import com.example.ui.components.MealStatusBadge
import com.example.ui.components.VegNonVegIndicator
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.DarkTextTertiary
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.LocalDayProfile
import com.example.ui.theme.NonVegRed
import com.example.ui.theme.UpNextAmber
import kotlinx.coroutines.launch
import java.time.LocalTime

@Composable
fun MessMenuScreen(
    currentDayIndex: Int,
    currentTime: LocalTime,
    currentMealStatus: MessMenuRepository.CurrentMealStatus,
    allDaysMenu: List<DayMessMenu>,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayProfile = LocalDayProfile.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = currentDayIndex, pageCount = { allDaysMenu.size })
    val currentMinutes = currentTime.hour * 60 + currentTime.minute

    // Sync pager with selected day index
    LaunchedEffect(currentDayIndex) {
        if (pagerState.currentPage != currentDayIndex) {
            pagerState.animateScrollToPage(currentDayIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        onDaySelected(pagerState.currentPage)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Countdown banner showing time remaining until next meal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mess_countdown_card")
                .clip(RoundedCornerShape(16.dp))
                .background(dayProfile.primaryAccent.copy(alpha = 0.10f))
                .border(1.dp, dayProfile.primaryAccent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(dayProfile.primaryAccent.copy(alpha = 0.20f))
                ) {
                    Icon(
                        imageVector = if (currentMealStatus.isAllTodayMealsEnded) Icons.Default.WbSunny else Icons.Default.Timer,
                        contentDescription = null,
                        tint = dayProfile.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (currentMealStatus.isAllTodayMealsEnded) "NEXT: TOMORROW'S BREAKFAST" else "MESS SCHEDULE LIVE",
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

        Spacer(modifier = Modifier.height(14.dp))

        // Day Selector Chips (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
        val dayScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(dayScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allDaysMenu.forEachIndexed { index, dayMenu ->
                val isSelected = index == pagerState.currentPage
                val isToday = index == currentMealStatus.dayMenu.dayIndex

                Box(
                    modifier = Modifier
                        .testTag("mess_day_chip_${dayMenu.dayName}")
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) dayProfile.primaryAccent else DarkSurfaceCard)
                        .border(
                            1.dp,
                            if (isSelected) dayProfile.primaryAccent else if (isToday) dayProfile.primaryAccent.copy(alpha = 0.5f) else DarkSurfaceBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dayMenu.dayName.take(3),
                            color = if (isSelected) Color.White else DarkTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isToday) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else dayProfile.primaryAccent)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Pager for swipeable meals Monday..Sunday
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("mess_horizontal_pager")
        ) { pageIndex ->
            val dayMenu = allDaysMenu[pageIndex]
            val isToday = pageIndex == currentMealStatus.dayMenu.dayIndex
            val isTomorrow = pageIndex == (currentMealStatus.dayMenu.dayIndex + 1) % 7

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("mess_meals_list_$pageIndex"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${dayMenu.dayName}'s Menu",
                                color = DarkTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "IIM Bodh Gaya • October 2026",
                                color = DarkTextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (isToday) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(dayProfile.primaryAccent.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "TODAY",
                                    color = dayProfile.primaryAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (isTomorrow && currentMealStatus.isAllTodayMealsEnded) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(dayProfile.primaryAccent.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "NEXT MEAL TOMORROW",
                                    color = dayProfile.primaryAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // If today's meals ended and viewing today, show notification card
                if (isToday && currentMealStatus.isAllTodayMealsEnded) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = dayProfile.primaryAccent.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, dayProfile.primaryAccent.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = dayProfile.primaryAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "All of today's meals have concluded",
                                        color = DarkTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tomorrow's breakfast starts at 8:00 AM. Swipe right or tap ${allDaysMenu[(currentDayIndex + 1) % 7].dayName.take(3)} to view it.",
                                        color = DarkTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                items(dayMenu.meals) { meal ->
                    val mealStatus = MessMenuRepository.resolveMealStatus(
                        meal = meal,
                        mealDayIndex = pageIndex,
                        currentDayIndex = currentMealStatus.dayMenu.dayIndex,
                        currentMinutes = currentMinutes
                    )

                    MealDetailCard(
                        meal = meal,
                        status = mealStatus,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Accompaniments Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = dayProfile.primaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Standard Campus Mess Accompaniments",
                                    color = DarkTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Fresh Salad: Cucumber, Onion, Carrot served at Lunch & Dinner\n" +
                                        "• Snacks: Freshly brewed Tea & Coffee (Milk + Coffee Powder Separate)\n" +
                                        "• Green/Red dots denote vegetarian and non-vegetarian recipes.",
                                color = DarkTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MealDetailCard(
    meal: MealMenu,
    status: MealStatus,
    modifier: Modifier = Modifier
) {
    val dayProfile = LocalDayProfile.current
    val isServing = status == MealStatus.SERVING_NOW
    val isEnded = status == MealStatus.ENDED

    val stripeColor = when {
        isServing -> LiveGreen
        status == MealStatus.UP_NEXT -> UpNextAmber
        else -> DarkSurfaceBorder
    }

    Card(
        modifier = modifier
            .testTag("meal_card_${meal.mealType.name.lowercase()}")
            .alpha(if (isEnded) 0.65f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRect(
                    color = stripeColor,
                    topLeft = Offset.Zero,
                    size = Size(width = 4.dp.toPx(), height = size.height)
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isEnded) DarkSurfaceCard.copy(alpha = 0.5f) else DarkSurfaceCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)) {
            // Header: Meal Name & Time Window
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = meal.mealType.displayName,
                        color = if (isEnded) DarkTextSecondary else DarkTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = DarkTextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = meal.mealType.timeWindow,
                            color = DarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                MealStatusBadge(status = status)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dish Items with Green/Red indicators
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                meal.dishes.forEach { dish ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VegNonVegIndicator(isNonVeg = dish.isNonVeg)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = dish.name,
                            color = if (dish.isNonVeg) Color(0xFFFCA5A5) else if (isEnded) DarkTextSecondary else DarkTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (dish.isNonVeg) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (dish.isNonVeg) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(Non-Veg)",
                                color = NonVegRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (meal.specialNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = meal.specialNote,
                    color = DarkTextSecondary,
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}
