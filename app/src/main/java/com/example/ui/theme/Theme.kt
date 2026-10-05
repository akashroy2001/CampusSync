package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek

val LocalDayProfile = staticCompositionLocalOf {
    DayThemeHelper.getProfile(DayOfWeek.THURSDAY)
}

@Composable
fun CampusSyncTheme(
    activeDayOfWeek: DayOfWeek = DayOfWeek.THURSDAY,
    content: @Composable () -> Unit
) {
    val dayProfile = DayThemeHelper.getProfile(activeDayOfWeek)

    val colorScheme = darkColorScheme(
        primary = dayProfile.primaryAccent,
        onPrimary = Color.White,
        primaryContainer = dayProfile.primaryContainer,
        onPrimaryContainer = dayProfile.onPrimaryContainer,
        secondary = dayProfile.primaryAccent.copy(alpha = 0.8f),
        onSecondary = Color.White,
        secondaryContainer = dayProfile.primaryContainer,
        onSecondaryContainer = dayProfile.onPrimaryContainer,
        background = DarkBackground,
        onBackground = DarkTextPrimary,
        surface = DarkSurface,
        onSurface = DarkTextPrimary,
        surfaceVariant = DarkSurfaceElevated,
        onSurfaceVariant = DarkTextSecondary,
        outline = DarkSurfaceBorder,
        outlineVariant = DarkSurfaceBorder.copy(alpha = 0.6f)
    )

    CompositionLocalProvider(LocalDayProfile provides dayProfile) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

