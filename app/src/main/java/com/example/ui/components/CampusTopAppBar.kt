package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LocalDayProfile
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CampusTopAppBar(
    currentDate: LocalDate,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayProfile = LocalDayProfile.current
    val formattedDate = currentDate.format(DateTimeFormatter.ofPattern("EEE, d MMM"))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .statusBarsPadding()
            .border(width = 0.5.dp, color = DarkSurfaceBorder)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title & Live Pulse Dot
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CampusSync",
                    color = DarkTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dayProfile.primaryAccent)
                )
            }

            // Right side: Date Pill & Settings Icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Today's Date Pill Badge
                Box(
                    modifier = Modifier
                        .testTag("date_pill_badge")
                        .clip(RoundedCornerShape(50.dp))
                        .background(DarkSurfaceBorder)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = formattedDate,
                        color = dayProfile.primaryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Settings Icon Button
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .testTag("settings_button")
                        .size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings & Updates",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}
