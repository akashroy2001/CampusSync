package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.ClassSlot
import com.example.data.model.ScheduleClass
import com.example.data.model.SlotStatus
import com.example.ui.screens.ClassSlotCard
import com.example.ui.theme.CampusSyncTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun schedule_card_render() {
    val sampleClass = ScheduleClass(
      dateStr = "03-09-2026",
      dayOfWeek = "Thursday",
      slot = ClassSlot.ALL_SLOTS[0],
      courseName = "Financial Reporting & Analysis",
      sessionNumber = "12",
      facultyName = "Dr. S. Mukherjee",
      status = SlotStatus.LIVE_NOW
    )

    composeTestRule.setContent {
      CampusSyncTheme {
        ClassSlotCard(classItem = sampleClass)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/schedule_card.png")
  }
}
