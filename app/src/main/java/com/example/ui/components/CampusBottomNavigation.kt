package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LocalDayProfile
import com.example.ui.viewmodel.NavigationTab

@Composable
fun CampusBottomNavigation(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayProfile = LocalDayProfile.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .border(
                width = 0.5.dp,
                color = DarkSurfaceBorder
            )
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab 1: Now Dashboard
        SleekNavItem(
            selected = selectedTab == NavigationTab.NOW_DASHBOARD,
            label = "Now",
            selectedIcon = Icons.Filled.Dashboard,
            unselectedIcon = Icons.Outlined.Dashboard,
            testTag = "nav_tab_now",
            onClick = { onTabSelected(NavigationTab.NOW_DASHBOARD) }
        )

        // Tab 2: Timetable Schedule
        SleekNavItem(
            selected = selectedTab == NavigationTab.SCHEDULE,
            label = "Schedule",
            selectedIcon = Icons.Filled.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth,
            testTag = "nav_tab_schedule",
            onClick = { onTabSelected(NavigationTab.SCHEDULE) }
        )

        // Tab 3: Attendance (NEW!)
        SleekNavItem(
            selected = selectedTab == NavigationTab.ATTENDANCE,
            label = "Attendance",
            selectedIcon = Icons.Filled.AssignmentTurnedIn,
            unselectedIcon = Icons.Outlined.AssignmentTurnedIn,
            testTag = "nav_tab_attendance",
            onClick = { onTabSelected(NavigationTab.ATTENDANCE) }
        )

        // Tab 4: Mess Menu
        SleekNavItem(
            selected = selectedTab == NavigationTab.MESS,
            label = "Mess",
            selectedIcon = Icons.Filled.Restaurant,
            unselectedIcon = Icons.Outlined.Restaurant,
            testTag = "nav_tab_mess",
            onClick = { onTabSelected(NavigationTab.MESS) }
        )
    }
}

@Composable
private fun SleekNavItem(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    val dayProfile = LocalDayProfile.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .testTag(testTag)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 28.dp)
            ) { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) dayProfile.primaryAccent.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = if (selected) dayProfile.primaryAccent else DarkTextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) dayProfile.primaryAccent else DarkTextSecondary,
            letterSpacing = 0.6.sp
        )
    }
}

