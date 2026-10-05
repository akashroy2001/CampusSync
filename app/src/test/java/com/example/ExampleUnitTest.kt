package com.example

import com.example.data.model.ClassSlot
import com.example.data.model.MealType
import com.example.data.model.Section
import com.example.data.repository.MessMenuRepository
import com.example.ui.theme.DayThemeHelper
import com.example.ui.theme.FridayViolet
import com.example.ui.theme.MondayIndigo
import com.example.ui.theme.SaturdayTeal
import com.example.ui.theme.SundayTerracotta
import com.example.ui.theme.ThursdayRoseCoral
import com.example.ui.theme.TuesdayEmerald
import com.example.ui.theme.WednesdayAmber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class ExampleUnitTest {

    @Test
    fun verifySectionModels() {
        assertEquals("Sec A", Section.SECTION_A.shortPill)
        assertEquals("Sec B", Section.SECTION_B.shortPill)
        assertEquals("Sec C", Section.SECTION_C.shortPill)
        assertEquals("Sec D", Section.SECTION_D.shortPill)

        assertEquals(Section.SECTION_B, Section.fromString("Section B"))
        assertEquals(Section.SECTION_C, Section.fromString("Sec C"))
        assertTrue(Section.SECTION_A.description.contains("MBA 2026-28"))
    }

    @Test
    fun verifyClassSlots() {
        assertTrue(ClassSlot.ALL_SLOTS.size >= 5)
        val slot1 = ClassSlot.ALL_SLOTS[0]
        assertEquals("09:15-10:45", slot1.timeRange)
        assertEquals(9 * 60 + 15, slot1.startMinutes)
        assertEquals(10 * 60 + 45, slot1.endMinutes)

        val slot5 = ClassSlot.ALL_SLOTS[4]
        assertEquals("17:00-18:30", slot5.timeRange)
    }

    @Test
    fun verifyDynamicDayColors() {
        assertEquals(MondayIndigo, DayThemeHelper.getProfile(DayOfWeek.MONDAY).primaryAccent)
        assertEquals(TuesdayEmerald, DayThemeHelper.getProfile(DayOfWeek.TUESDAY).primaryAccent)
        assertEquals(WednesdayAmber, DayThemeHelper.getProfile(DayOfWeek.WEDNESDAY).primaryAccent)
        assertEquals(ThursdayRoseCoral, DayThemeHelper.getProfile(DayOfWeek.THURSDAY).primaryAccent)
        assertEquals(FridayViolet, DayThemeHelper.getProfile(DayOfWeek.FRIDAY).primaryAccent)
        assertEquals(SaturdayTeal, DayThemeHelper.getProfile(DayOfWeek.SATURDAY).primaryAccent)
        assertEquals(SundayTerracotta, DayThemeHelper.getProfile(DayOfWeek.SUNDAY).primaryAccent)
    }

    @Test
    fun verifyMessMenuDaysAndDishes() {
        assertEquals(7, MessMenuRepository.ALL_DAYS_MENU.size)

        // Monday dinner should have Chicken Kolhapuri marked as non-veg
        val monday = MessMenuRepository.getMenuForDayName("Monday")
        val mondayDinner = monday.meals.first { it.mealType == MealType.DINNER }
        val chickenKolhapuri = mondayDinner.dishes.firstOrNull { it.name.contains("Chicken Kolhapuri", ignoreCase = true) }
        assertNotNull("Chicken Kolhapuri should be in Monday dinner", chickenKolhapuri)
        assertTrue("Chicken Kolhapuri should be non-veg", chickenKolhapuri!!.isNonVeg)

        // Sunday lunch should have Chicken Biryani as non-veg and Veg Biryani as veg
        val sunday = MessMenuRepository.getMenuForDayName("Sunday")
        val sundayLunch = sunday.meals.first { it.mealType == MealType.LUNCH }
        val chickenBiryani = sundayLunch.dishes.firstOrNull { it.name.contains("Chicken Biryani", ignoreCase = true) }
        assertNotNull(chickenBiryani)
        assertTrue(chickenBiryani!!.isNonVeg)

        val vegBiryani = sundayLunch.dishes.firstOrNull { it.name.contains("Veg Biryani", ignoreCase = true) }
        assertNotNull(vegBiryani)
        assertTrue(!vegBiryani!!.isNonVeg)
    }

    @Test
    fun verifyMealWindows() {
        assertEquals(8 * 60, MealType.BREAKFAST.startMinutes)
        assertEquals(10 * 60, MealType.BREAKFAST.endMinutes)

        assertEquals(12 * 60, MealType.LUNCH.startMinutes)
        assertEquals(14 * 60 + 15, MealType.LUNCH.endMinutes)

        assertEquals(17 * 60, MealType.SNACKS.startMinutes)
        assertEquals(18 * 60 + 30, MealType.SNACKS.endMinutes)

        assertEquals(19 * 60 + 45, MealType.DINNER.startMinutes)
        assertEquals(22 * 60, MealType.DINNER.endMinutes)
    }

    @Test
    fun verifyCurrentMealStatusLogic() {
        // At 12:30 PM (750 minutes) Lunch should be active
        val status = MessMenuRepository.getCurrentMealStatus(750, 0)
        assertTrue("Lunch should be active at 12:30 PM", status.isMealActiveNow)
        assertEquals(MealType.LUNCH, status.activeMeal?.mealType)

        // At 7:00 AM (420 minutes) Breakfast should be up next
        val morningStatus = MessMenuRepository.getCurrentMealStatus(420, 0)
        assertTrue(!morningStatus.isMealActiveNow)
        assertEquals(MealType.BREAKFAST, morningStatus.nextMeal?.mealType)
    }
}

