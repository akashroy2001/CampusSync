package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MealStatus
import com.example.data.model.SlotStatus
import com.example.ui.theme.CompletedMuted
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.DarkTextTertiary
import com.example.ui.theme.EggYellow
import com.example.ui.theme.HolidayPurple
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.LocalDayProfile
import com.example.ui.theme.NonVegRed
import com.example.ui.theme.UpNextAmber
import com.example.ui.theme.VegGreen

@Composable
fun ClassStatusBadge(
    status: SlotStatus,
    modifier: Modifier = Modifier
) {
    val dayProfile = LocalDayProfile.current

    when (status) {
        SlotStatus.LIVE_NOW -> {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .testTag("badge_live_now")
                    .padding(vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(dayProfile.primaryAccent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE NOW",
                    color = dayProfile.primaryAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
        }
        SlotStatus.UP_NEXT -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .testTag("badge_up_next")
                    .clip(RoundedCornerShape(4.dp))
                    .background(UpNextAmber.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Up Next",
                    color = UpNextAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }
        }
        SlotStatus.COMPLETED -> {
            Text(
                text = "Finished",
                color = DarkTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkSurfaceBorder)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        SlotStatus.HOLIDAY -> {
            Text(
                text = "Special Event",
                color = Color(0xFFDDD6FE),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x338B5CF6))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        SlotStatus.FREE_PERIOD -> {
            Text(
                text = "Free Slot",
                color = DarkTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                modifier = modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkSurfaceBorder.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        SlotStatus.UPCOMING -> {
            Text(
                text = "Scheduled",
                color = DarkTextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                modifier = modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkSurfaceBorder.copy(alpha = 0.4f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun MealStatusBadge(
    status: MealStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        MealStatus.SERVING_NOW -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .testTag("badge_serving_now")
                    .padding(vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(LiveGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SERVING NOW",
                    color = LiveGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
        MealStatus.UP_NEXT -> {
            Text(
                text = "Up Next",
                color = UpNextAmber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(UpNextAmber.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        MealStatus.ENDED -> {
            Text(
                text = "Ended",
                color = DarkTextSecondary,
                fontSize = 10.sp,
                modifier = modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkSurfaceBorder)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        MealStatus.UPCOMING -> {
            Text(
                text = "Upcoming",
                color = DarkTextTertiary,
                fontSize = 10.sp,
                modifier = modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkSurfaceBorder.copy(alpha = 0.4f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Standard Food Safety green/red veg vs non-veg indicator dot inside square
 */
@Composable
fun VegNonVegIndicator(
    isNonVeg: Boolean,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (isNonVeg) NonVegRed else VegGreen
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .testTag(if (isNonVeg) "indicator_non_veg" else "indicator_veg")
            .size(15.dp)
            .border(1.2.dp, indicatorColor, RoundedCornerShape(3.dp))
            .background(Color(0x11000000))
            .padding(2.5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
    }
}
