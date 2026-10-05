package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _onboarded = MutableStateFlow(true) // No section onboarding needed! Direct access.
    val onboarded: StateFlow<Boolean> = _onboarded.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(getNotificationsEnabledInternal())
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _leadTimeMinutes = MutableStateFlow(prefs.getInt(KEY_LEAD_TIME, 10))
    val leadTimeMinutes: StateFlow<Int> = _leadTimeMinutes.asStateFlow()

    private fun getNotificationsEnabledInternal(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun completeOnboarding() {
        prefs.edit()
            .putBoolean(KEY_IS_ONBOARDED, true)
            .apply()
        _onboarded.value = true
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setLeadTimeMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_LEAD_TIME, minutes).apply()
        _leadTimeMinutes.value = minutes
    }

    fun markCustomScheduleUpdated(hasCustom: Boolean) {
        prefs.edit()
            .putBoolean(KEY_HAS_CUSTOM_SCHEDULE, hasCustom)
            .putLong(KEY_CUSTOM_SCHEDULE_TIME, if (hasCustom) System.currentTimeMillis() else 0L)
            .apply()
    }

    fun hasCustomSchedule(): Boolean {
        return prefs.getBoolean(KEY_HAS_CUSTOM_SCHEDULE, false)
    }

    fun getCustomScheduleTimestamp(): Long {
        return prefs.getLong(KEY_CUSTOM_SCHEDULE_TIME, 0L)
    }

    companion object {
        private const val PREFS_NAME = "campussync_prefs"
        private const val KEY_IS_ONBOARDED = "is_onboarded"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_LEAD_TIME = "lead_time_minutes"
        private const val KEY_HAS_CUSTOM_SCHEDULE = "has_custom_schedule"
        private const val KEY_CUSTOM_SCHEDULE_TIME = "custom_schedule_time"

        @Volatile
        private var INSTANCE: UserPreferencesManager? = null

        fun getInstance(context: Context): UserPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
