package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.repository.MessMenuRepository
import com.example.ui.components.CampusBottomNavigation
import com.example.ui.components.CampusTopAppBar
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.MessMenuScreen
import com.example.ui.screens.NowDashboardScreen
import com.example.ui.screens.SettingsBottomSheet
import com.example.ui.screens.TimetableScreen
import com.example.ui.theme.CampusSyncTheme
import com.example.ui.theme.DarkBackground
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.NavigationTab
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val selectedDate by viewModel.selectedDate.collectAsState()

            CampusSyncTheme(activeDayOfWeek = selectedDate.dayOfWeek) {
                CampusSyncApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusSyncApp(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val scheduleViewMode by viewModel.scheduleViewMode.collectAsState()
    val daySchedule by viewModel.currentDaySchedule.collectAsState()
    val allSchedules by viewModel.allSchedules.collectAsState()
    val activeClassInfo by viewModel.activeClassInfo.collectAsState()
    val currentMealStatus by viewModel.currentMealStatus.collectAsState()
    val selectedMessDayIndex by viewModel.selectedMessDayIndex.collectAsState()
    val isLoadingSchedule by viewModel.isLoadingSchedule.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleNotifications(true)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Show snackbar feedback
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    Scaffold(
        topBar = {
            CampusTopAppBar(
                currentDate = selectedDate,
                onSettingsClick = { showSettingsSheet = true }
            )
        },
        bottomBar = {
            CampusBottomNavigation(
                selectedTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = DarkBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            Crossfade(
                targetState = currentTab,
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    NavigationTab.NOW_DASHBOARD -> {
                        NowDashboardScreen(
                            selectedDate = LocalDate.now(),
                            activeClassInfo = activeClassInfo,
                            currentMealStatus = currentMealStatus,
                            onNavigateToSchedule = { viewModel.selectTab(NavigationTab.SCHEDULE) },
                            onNavigateToMess = { viewModel.selectTab(NavigationTab.MESS) },
                            onNavigateToAttendance = { viewModel.selectTab(NavigationTab.ATTENDANCE) }
                        )
                    }
                    NavigationTab.SCHEDULE -> {
                        TimetableScreen(
                            selectedDate = selectedDate,
                            viewMode = scheduleViewMode,
                            daySchedule = daySchedule,
                            weekSchedules = allSchedules,
                            isLoading = isLoadingSchedule,
                            searchQuery = searchQuery,
                            onViewModeChange = { viewModel.setScheduleViewMode(it) },
                            onDateSelect = { viewModel.setSelectedDate(it) },
                            onJumpToToday = { viewModel.jumpToToday() },
                            onSearchChange = { viewModel.setSearchQuery(it) }
                        )
                    }
                    NavigationTab.ATTENDANCE -> {
                        AttendanceScreen(
                            attendance = attendance,
                            onSetAttendedCount = { courseName, attended ->
                                viewModel.setSubjectAttendedCount(courseName, attended)
                            },
                            onResetToOfficial = { viewModel.resetAttendanceToOfficial() },
                            onUpdateSubject = { courseName, att, cond ->
                                viewModel.updateSingleSubjectAttendance(courseName, att, cond)
                            },
                            onDeleteCourse = { courseName ->
                                viewModel.deleteCourse(courseName)
                            }
                        )
                    }
                    NavigationTab.MESS -> {
                        MessMenuScreen(
                            currentDayIndex = selectedMessDayIndex,
                            currentTime = currentTime,
                            currentMealStatus = currentMealStatus,
                            allDaysMenu = MessMenuRepository.ALL_DAYS_MENU,
                            onDaySelected = { viewModel.setSelectedMessDayIndex(it) }
                        )
                    }
                }
            }
        }
    }

    // In-App Settings & Spreadsheet updates bottom sheet
    if (showSettingsSheet) {
        SettingsBottomSheet(
            notificationsEnabled = notificationsEnabled,
            isUsingCustomSchedule = viewModel.isUsingCustomSchedule(),
            onToggleNotifications = { viewModel.toggleNotifications(it) },
            onSendTestNotification = { viewModel.sendTestNotification() },
            onFileSelected = { uri ->
                viewModel.importNewSpreadsheet(uri)
                showSettingsSheet = false
            },
            onResetToDefault = {
                viewModel.resetScheduleToDefault()
                showSettingsSheet = false
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}
