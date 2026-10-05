package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek

// Day of week dynamic accent colors
val MondayIndigo = Color(0xFF4F46E5)
val TuesdayEmerald = Color(0xFF059669)
val WednesdayAmber = Color(0xFFD97706)
val ThursdayRoseCoral = Color(0xFFE11D48)
val FridayViolet = Color(0xFF7C3AED)
val SaturdayTeal = Color(0xFF0891B2)
val SundayTerracotta = Color(0xFFEA580C)

// Sleek Interface Theme Foundations (Design HTML tokens)
val DarkBackground = Color(0xFF1A1C1E)
val DarkSurface = Color(0xFF1A1C1E)
val DarkSurfaceElevated = Color(0xFF3D3F43)
val DarkSurfaceCard = Color(0xFF2D2F31)
val DarkSurfaceBorder = Color(0xFF44474E)

val DarkTextPrimary = Color(0xFFE2E2E6)
val DarkTextSecondary = Color(0xFFC4C6CF)
val DarkTextTertiary = Color(0xFF8E9099)
val SectionBadgeText = Color(0xFFD1E1FF)

// Status Colors
val LiveGreen = Color(0xFF10B981)
val UpNextAmber = Color(0xFFD97706)
val CompletedMuted = Color(0xFF6B7280)
val HolidayPurple = Color(0xFF8B5CF6)

// Mess Meal Indicators
val VegGreen = Color(0xFF22C55E)
val NonVegRed = Color(0xFFEF4444)
val EggYellow = Color(0xFFEAB308)

data class DayColorProfile(
    val dayOfWeek: DayOfWeek,
    val dayName: String,
    val primaryAccent: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val glowColor: Color
)

object DayThemeHelper {
    fun getProfile(day: DayOfWeek): DayColorProfile {
        return when (day) {
            DayOfWeek.MONDAY -> DayColorProfile(
                dayOfWeek = day,
                dayName = "Monday",
                primaryAccent = MondayIndigo,
                primaryContainer = Color(0x334F46E5),
                onPrimaryContainer = Color(0xFFC7D2FE),
                glowColor = Color(0x664F46E5)
            )
            DayOfWeek.TUESDAY -> DayColorProfile(
                dayOfWeek = day,
                dayName = "Tuesday",
                primaryAccent = TuesdayEmerald,
                primaryContainer = Color(0x33059669),
                onPrimaryContainer = Color(0xFFA7F3D0),
                glowColor = Color(0x66059669)
            )
            DayOfWeek.WEDNESDAY -> DayColorProfile(
                dayOfWeek = day,
                dayName = "Wednesday",
                primaryAccent = WednesdayAmber,
                primaryContainer = Color(0x33D97706),
                onPrimaryContainer = Color(0xFFFDE68A),
                glowColor = Color(0x66D97706)
            )
            DayOfWeek.THURSDAY -> DayColorProfile(
                dayOfWeek = day,
                dayName = "Thursday",
                primaryAccent = ThursdayRoseCoral,
                primaryContainer = Color(0x33E11D48),
                onPrimaryContainer = Color(0xFFFECDD3),
                glowColor = Color(0x66E11D48)
            )
            DayOfWeek.FRIDAY -> DayColorProfile(
                dayOfWeek = day,
                dayName = "Friday",
                primaryAccent = FridayViolet,
                primaryContainer = Color(0x337C3AED),
                onPrimaryContainer = Color(0xFFDDD6FE),
                glowColor = Color(0x667C3AED)
            )
            DayOfWeek.SATURDAY -> DayColorProfile(
                dayOfWeek = day,
                dayName = "Saturday",
                primaryAccent = SaturdayTeal,
                primaryContainer = Color(0x330891B2),
                onPrimaryContainer = Color(0xFFA5F3FC),
                glowColor = Color(0x660891B2)
            )
            DayOfWeek.SUNDAY -> DayColorProfile(
                dayOfWeek = day,
                dayName = "Sunday",
                primaryAccent = SundayTerracotta,
                primaryContainer = Color(0x33EA580C),
                onPrimaryContainer = Color(0xFFFED7AA),
                glowColor = Color(0x66EA580C)
            )
        }
    }
}

