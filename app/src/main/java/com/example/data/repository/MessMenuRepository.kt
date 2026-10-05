package com.example.data.repository

import com.example.data.model.DayMessMenu
import com.example.data.model.MealMenu
import com.example.data.model.MealStatus
import com.example.data.model.MealType
import com.example.data.model.MessDish
import java.time.LocalTime

object MessMenuRepository {

    private val NON_VEG_KEYWORDS = listOf(
        "chicken", "egg", "omelette", "mutton", "fish"
    )

    private fun dish(name: String, explicitNonVeg: Boolean? = null): MessDish {
        val isNonVeg = explicitNonVeg ?: NON_VEG_KEYWORDS.any { name.lowercase().contains(it) }
        return MessDish(name.trim(), isNonVeg)
    }

    private val MONDAY_MENU = DayMessMenu(
        dayName = "Monday",
        dayIndex = 0,
        meals = listOf(
            MealMenu(
                mealType = MealType.BREAKFAST,
                dishes = listOf(
                    dish("Idli"),
                    dish("Vada"),
                    dish("Sambar"),
                    dish("Peanut Chutney"),
                    dish("Bread"),
                    dish("Boiled Egg", explicitNonVeg = true),
                    dish("Butter, Jam"),
                    dish("Chocos, Fruits"),
                    dish("Tea, Milk and Coffee")
                )
            ),
            MealMenu(
                mealType = MealType.LUNCH,
                dishes = listOf(
                    dish("Chole Masala"),
                    dish("Poriyal"),
                    dish("Rice"),
                    dish("Roti"),
                    dish("Papad"),
                    dish("Palak Chana Dal"),
                    dish("Salad and Pickle"),
                    dish("Butter Milk")
                )
            ),
            MealMenu(
                mealType = MealType.SNACKS,
                dishes = listOf(
                    dish("Pasta"),
                    dish("Tea, Coffee and Milk")
                ),
                specialNote = "Snacks — Tea, Coffee (Milk + Coffee Powder Separate)"
            ),
            MealMenu(
                mealType = MealType.DINNER,
                dishes = listOf(
                    dish("Paneer Do Pyaza"),
                    dish("Veg Pulao"),
                    dish("Yellow Dal"),
                    dish("Roti"),
                    dish("Chicken Kolhapuri", explicitNonVeg = true),
                    dish("Ice Cream and Salad")
                )
            )
        )
    )

    private val TUESDAY_MENU = DayMessMenu(
        dayName = "Tuesday",
        dayIndex = 1,
        meals = listOf(
            MealMenu(
                mealType = MealType.BREAKFAST,
                dishes = listOf(
                    dish("Aloo Paratha"),
                    dish("Tomato Chutney"),
                    dish("Pickle"),
                    dish("Peanut Butter"),
                    dish("Bread, Jam"),
                    dish("Cornflakes, Fruits"),
                    dish("Sprouts"),
                    dish("Tea, Milk and Coffee")
                )
            ),
            MealMenu(
                mealType = MealType.LUNCH,
                dishes = listOf(
                    dish("Jeera Aloo"),
                    dish("Rajma"),
                    dish("Chana Dal"),
                    dish("Rice"),
                    dish("Roti"),
                    dish("Dal Fry"),
                    dish("Salad and Pickle"),
                    dish("Lassi")
                )
            ),
            MealMenu(
                mealType = MealType.SNACKS,
                dishes = listOf(
                    dish("Pani Puri"),
                    dish("Green Chutney"),
                    dish("Red Chutney"),
                    dish("Tea, Coffee and Milk")
                ),
                specialNote = "Snacks — Tea, Coffee (Milk + Coffee Powder Separate)"
            ),
            MealMenu(
                mealType = MealType.DINNER,
                dishes = listOf(
                    dish("Veg Manchurian"),
                    dish("Schezwan Rice"),
                    dish("Roti"),
                    dish("Masoor Dal"),
                    dish("Papad"),
                    dish("Rasmalai"),
                    dish("Pickle and Salad")
                )
            )
        )
    )

    private val WEDNESDAY_MENU = DayMessMenu(
        dayName = "Wednesday",
        dayIndex = 2,
        meals = listOf(
            MealMenu(
                mealType = MealType.BREAKFAST,
                dishes = listOf(
                    dish("Uttapam"),
                    dish("Peanut Chutney"),
                    dish("Bread"),
                    dish("Boiled Egg", explicitNonVeg = true),
                    dish("Butter, Jam"),
                    dish("Chocos, Fruits"),
                    dish("Tea, Milk and Coffee")
                )
            ),
            MealMenu(
                mealType = MealType.LUNCH,
                dishes = listOf(
                    dish("Punjabi Chole Bhature"),
                    dish("Aloo Masala"),
                    dish("Rice"),
                    dish("Fryums"),
                    dish("Boondi Raita"),
                    dish("Salad and Pickle")
                )
            ),
            MealMenu(
                mealType = MealType.SNACKS,
                dishes = listOf(
                    dish("Vada Pav"),
                    dish("Chopped Onion"),
                    dish("Tea, Coffee and Milk")
                ),
                specialNote = "Snacks — Tea, Coffee (Milk + Coffee Powder Separate)"
            ),
            MealMenu(
                mealType = MealType.DINNER,
                dishes = listOf(
                    dish("Matar Paneer"),
                    dish("Dal Fry"),
                    dish("Rice"),
                    dish("Paratha"),
                    dish("Mughlai Chicken", explicitNonVeg = true),
                    dish("Gulab Jamun and Salad")
                )
            )
        )
    )

    private val THURSDAY_MENU = DayMessMenu(
        dayName = "Thursday",
        dayIndex = 3,
        meals = listOf(
            MealMenu(
                mealType = MealType.BREAKFAST,
                dishes = listOf(
                    dish("Puri"),
                    dish("Aloo ki Sabzi"),
                    dish("Bread"),
                    dish("Peanut Butter, Jam"),
                    dish("Cornflakes, Sprouts"),
                    dish("Fruits"),
                    dish("Tea, Milk and Coffee")
                )
            ),
            MealMenu(
                mealType = MealType.LUNCH,
                dishes = listOf(
                    dish("Rajma"),
                    dish("Rice"),
                    dish("Roti"),
                    dish("Chana Dal"),
                    dish("Egg Curry", explicitNonVeg = true),
                    dish("Fryums"),
                    dish("Salad and Pickle")
                )
            ),
            MealMenu(
                mealType = MealType.SNACKS,
                dishes = listOf(
                    dish("Samosa"),
                    dish("Chole"),
                    dish("Green Chutney"),
                    dish("Red Chutney"),
                    dish("Tea, Coffee and Milk")
                ),
                specialNote = "Snacks — Tea, Coffee (Milk + Coffee Powder Separate)"
            ),
            MealMenu(
                mealType = MealType.DINNER,
                dishes = listOf(
                    dish("Aloo Tamatar Matar"),
                    dish("Curd Rice"),
                    dish("Roti"),
                    dish("Chana Dal Tadka"),
                    dish("Payasam"),
                    dish("Salad")
                )
            )
        )
    )

    private val FRIDAY_MENU = DayMessMenu(
        dayName = "Friday",
        dayIndex = 4,
        meals = listOf(
            MealMenu(
                mealType = MealType.BREAKFAST,
                dishes = listOf(
                    dish("Sewaiyan Upma"),
                    dish("Peanut Chutney"),
                    dish("Bread"),
                    dish("Omelette", explicitNonVeg = true),
                    dish("Butter, Jam"),
                    dish("Cornflakes, Fruits"),
                    dish("Tea, Milk and Coffee")
                )
            ),
            MealMenu(
                mealType = MealType.LUNCH,
                dishes = listOf(
                    dish("Aloo Parwal Fry"),
                    dish("Poriyal"),
                    dish("Sambar"),
                    dish("Lemon Rice with Peanuts"),
                    dish("Roti"),
                    dish("Fryums"),
                    dish("Salad, Pickle"),
                    dish("Beetroot Raita")
                )
            ),
            MealMenu(
                mealType = MealType.SNACKS,
                dishes = listOf(
                    dish("Aloo Sandwich"),
                    dish("Tomato Sauce"),
                    dish("Tea, Coffee and Milk")
                ),
                specialNote = "Snacks — Tea, Coffee (Milk + Coffee Powder Separate)"
            ),
            MealMenu(
                mealType = MealType.DINNER,
                dishes = listOf(
                    dish("Chilli Paneer"),
                    dish("Schezwan Rice"),
                    dish("Roti"),
                    dish("Chilli Garlic Chicken", explicitNonVeg = true),
                    dish("Jalebi and Salad")
                )
            )
        )
    )

    private val SATURDAY_MENU = DayMessMenu(
        dayName = "Saturday",
        dayIndex = 5,
        meals = listOf(
            MealMenu(
                mealType = MealType.BREAKFAST,
                dishes = listOf(
                    dish("Palak Paratha"),
                    dish("Kala Chana"),
                    dish("Boiled Egg", explicitNonVeg = true),
                    dish("Pickle"),
                    dish("Bread, Peanut Butter & Jam"),
                    dish("Chocos, Fruits"),
                    dish("Tea, Milk and Coffee")
                )
            ),
            MealMenu(
                mealType = MealType.LUNCH,
                dishes = listOf(
                    dish("Kadhi Pakoda"),
                    dish("Aloo Bhujiya"),
                    dish("Rasam"),
                    dish("Rice"),
                    dish("Roti"),
                    dish("Salad"),
                    dish("Papad and Pickle")
                )
            ),
            MealMenu(
                mealType = MealType.SNACKS,
                dishes = listOf(
                    dish("Bread Pakoda"),
                    dish("Green Chutney"),
                    dish("Tomato Sauce"),
                    dish("Tea, Coffee and Milk")
                ),
                specialNote = "Snacks — Tea, Coffee (Milk + Coffee Powder Separate)"
            ),
            MealMenu(
                mealType = MealType.DINNER,
                dishes = listOf(
                    dish("Khichdi"),
                    dish("Aloo Baigan Choka"),
                    dish("Roti"),
                    dish("Veg Kolhapuri"),
                    dish("Fryums"),
                    dish("Besan Ladoo"),
                    dish("Pickle and Salad")
                )
            )
        )
    )

    private val SUNDAY_MENU = DayMessMenu(
        dayName = "Sunday",
        dayIndex = 6,
        meals = listOf(
            MealMenu(
                mealType = MealType.BREAKFAST,
                dishes = listOf(
                    dish("Masala Dosa"),
                    dish("Sambar"),
                    dish("Peanut Chutney"),
                    dish("Boiled Egg", explicitNonVeg = true),
                    dish("Bread"),
                    dish("Butter & Jam"),
                    dish("Cornflakes, Fruits"),
                    dish("Tea, Milk and Coffee")
                )
            ),
            MealMenu(
                mealType = MealType.LUNCH,
                dishes = listOf(
                    dish("Veg Biryani"),
                    dish("Paneer Butter Masala"),
                    dish("Chicken Biryani", explicitNonVeg = true),
                    dish("Salan", explicitNonVeg = true),
                    dish("Koshimbir, Salad")
                )
            ),
            MealMenu(
                mealType = MealType.SNACKS,
                dishes = listOf(
                    dish("Poha"),
                    dish("Sev"),
                    dish("Chopped Onion"),
                    dish("Tea, Coffee and Milk")
                ),
                specialNote = "Snacks — Tea, Coffee (Milk + Coffee Powder Separate)"
            ),
            MealMenu(
                mealType = MealType.DINNER,
                dishes = listOf(
                    dish("Kala Chana"),
                    dish("Jeera Aloo"),
                    dish("Masoor Dal"),
                    dish("Rice"),
                    dish("Poori"),
                    dish("Suji Halwa"),
                    dish("Salad")
                )
            )
        )
    )

    val ALL_DAYS_MENU = listOf(
        MONDAY_MENU,
        TUESDAY_MENU,
        WEDNESDAY_MENU,
        THURSDAY_MENU,
        FRIDAY_MENU,
        SATURDAY_MENU,
        SUNDAY_MENU
    )

    fun getMenuForDayIndex(dayIndex: Int): DayMessMenu {
        val safeIndex = (dayIndex % 7 + 7) % 7
        return ALL_DAYS_MENU[safeIndex]
    }

    fun getMenuForDayName(dayName: String): DayMessMenu {
        return ALL_DAYS_MENU.firstOrNull { it.dayName.equals(dayName, ignoreCase = true) }
            ?: MONDAY_MENU
    }

    /**
     * Determines the active or upcoming meal for the current time
     */
    data class CurrentMealStatus(
        val activeMeal: MealMenu?,
        val nextMeal: MealMenu?,
        val displayedMeal: MealMenu?,
        val dayMenu: DayMessMenu,
        val displayedMealDayName: String,
        val isMealActiveNow: Boolean,
        val isAllTodayMealsEnded: Boolean,
        val isNextMealTomorrow: Boolean,
        val bannerText: String,
        val timeDiffMinutes: Int
    )

    fun getCurrentMealStatus(currentMinutes: Int, dayIndex: Int): CurrentMealStatus {
        val todayMenu = getMenuForDayIndex(dayIndex)
        val tomorrowMenu = getMenuForDayIndex(dayIndex + 1)
        val todayMeals = todayMenu.meals

        var activeMeal: MealMenu? = null
        var nextMeal: MealMenu? = null
        var isAllTodayMealsEnded = false
        var isNextMealTomorrow = false

        // 1. Check if inside any meal window today
        for (meal in todayMeals) {
            if (currentMinutes in meal.mealType.startMinutes..meal.mealType.endMinutes) {
                activeMeal = meal
                break
            }
        }

        // 2. If an active meal is currently serving, find the meal after it
        if (activeMeal != null) {
            val activeIdx = todayMeals.indexOf(activeMeal)
            if (activeIdx >= 0 && activeIdx + 1 < todayMeals.size) {
                nextMeal = todayMeals[activeIdx + 1]
            } else {
                // Active meal is Dinner, so next meal after this will be tomorrow's Breakfast
                nextMeal = tomorrowMenu.meals.firstOrNull { it.mealType == MealType.BREAKFAST }
                isNextMealTomorrow = true
            }
        } else {
            // 3. Not in an active meal window: find the first meal whose start is in the future today
            for (meal in todayMeals) {
                if (currentMinutes < meal.mealType.startMinutes) {
                    nextMeal = meal
                    break
                }
            }

            // 4. If all meals today have ended (e.g. past dinner at 22:00 / 10:00 PM)
            if (nextMeal == null) {
                isAllTodayMealsEnded = true
                isNextMealTomorrow = true
                // Show tomorrow's breakfast!
                nextMeal = tomorrowMenu.meals.firstOrNull { it.mealType == MealType.BREAKFAST }
                    ?: tomorrowMenu.meals.firstOrNull()
            }
        }

        val displayed = activeMeal ?: nextMeal
        val displayedDayName = if (activeMeal != null) {
            "Today"
        } else if (isNextMealTomorrow) {
            "Tomorrow (${tomorrowMenu.dayName})"
        } else {
            "Today"
        }

        val bannerText: String
        val timeDiff: Int

        if (activeMeal != null) {
            val remaining = activeMeal.mealType.endMinutes - currentMinutes
            val hrs = remaining / 60
            val mins = remaining % 60
            val timeFormatted = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
            bannerText = "${activeMeal.mealType.displayName} is serving now • Ends in $timeFormatted"
            timeDiff = remaining
        } else if (nextMeal != null) {
            val startMin = nextMeal.mealType.startMinutes
            val diff = if (!isNextMealTomorrow && startMin >= currentMinutes) {
                startMin - currentMinutes
            } else {
                // Tomorrow's meal
                (24 * 60 - currentMinutes) + startMin
            }
            val hrs = diff / 60
            val mins = diff % 60
            val timeFormatted = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"

            bannerText = if (isAllTodayMealsEnded) {
                "Today's meals are done • Tomorrow's Breakfast starts in $timeFormatted (8:00 AM)"
            } else {
                "${nextMeal.mealType.displayName} starts in $timeFormatted (${nextMeal.mealType.timeWindow})"
            }
            timeDiff = diff
        } else {
            bannerText = "Mess is currently closed"
            timeDiff = 0
        }

        return CurrentMealStatus(
            activeMeal = activeMeal,
            nextMeal = nextMeal,
            displayedMeal = displayed,
            dayMenu = todayMenu,
            displayedMealDayName = displayedDayName,
            isMealActiveNow = activeMeal != null,
            isAllTodayMealsEnded = isAllTodayMealsEnded,
            isNextMealTomorrow = isNextMealTomorrow,
            bannerText = bannerText,
            timeDiffMinutes = timeDiff
        )
    }

    /**
     * Resolves the real-time status of a meal given the day index and current minutes
     */
    fun resolveMealStatus(
        meal: MealMenu,
        mealDayIndex: Int,
        currentDayIndex: Int,
        currentMinutes: Int
    ): MealStatus {
        if (mealDayIndex < currentDayIndex) {
            return MealStatus.ENDED
        }
        if (mealDayIndex > currentDayIndex) {
            // Check if this is tomorrow's breakfast when today's meals have ended
            if (mealDayIndex == (currentDayIndex + 1) % 7 && meal.mealType == MealType.BREAKFAST) {
                val currentStatus = getCurrentMealStatus(currentMinutes, currentDayIndex)
                if (currentStatus.isAllTodayMealsEnded) {
                    return MealStatus.UP_NEXT
                }
            }
            return MealStatus.UPCOMING
        }

        // Today's meal:
        return when {
            currentMinutes in meal.mealType.startMinutes..meal.mealType.endMinutes -> MealStatus.SERVING_NOW
            currentMinutes > meal.mealType.endMinutes -> MealStatus.ENDED
            else -> {
                val status = getCurrentMealStatus(currentMinutes, currentDayIndex)
                if (status.nextMeal?.mealType == meal.mealType) {
                    MealStatus.UP_NEXT
                } else {
                    MealStatus.UPCOMING
                }
            }
        }
    }
}
