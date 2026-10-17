package com.georgewitte.stillgood.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FoodTest {
    private val today = LocalDate.of(2026, 10, 17).toEpochDay()
    private fun item(id: String, name: String, addedDaysAgo: Long, keeps: Int) =
        FoodItem(id, name, "🍲", today - addedDaysAgo, keeps)

    @Test
    fun statusLabelsAndUrgency() {
        assertEquals(FoodStatus(4, Urgency.FRESH, "4 days"), Food.status(item("a", "Soup", 0, 4), today))
        assertEquals(FoodStatus(2, Urgency.FRESH, "2 days"), Food.status(item("a", "Soup", 2, 4), today))
        assertEquals(FoodStatus(1, Urgency.SOON, "Tomorrow"), Food.status(item("a", "Soup", 3, 4), today))
        assertEquals(FoodStatus(0, Urgency.TODAY, "Eat today"), Food.status(item("a", "Soup", 4, 4), today))
        assertEquals(FoodStatus(-1, Urgency.PAST, "Past date"), Food.status(item("a", "Soup", 5, 4), today))
    }

    @Test
    fun eatByDateIsAddedPlusDays() {
        val lasagna = FoodItem("x", "Lasagna", "🍲", LocalDate.of(2026, 10, 14).toEpochDay(), 4)
        assertEquals(LocalDate.of(2026, 10, 18), LocalDate.ofEpochDay(lasagna.eatByEpochDay))
    }

    @Test
    fun sortsSoonestFirstThenByName() {
        val items = listOf(
            item("1", "rice", 0, 4),
            item("2", "Chicken", 1, 2),
            item("3", "Apple slices", 1, 2),
            item("4", "Eggs", 0, 7),
        )
        assertEquals(listOf("3", "2", "1", "4"), Food.sortedForDisplay(items).map { it.id })
    }

    @Test
    fun countsItemsNeedingAttention() {
        val items = listOf(item("1", "A", 4, 4), item("2", "B", 9, 4), item("3", "C", 3, 4), item("4", "D", 0, 4))
        assertEquals(2, Food.needsAttention(items, today))
    }

    @Test
    fun tallyPercent() {
        assertNull(Tally(0, 0).savedPercent)
        assertEquals(80, Tally(12, 3).savedPercent)
        assertEquals(67, Tally(2, 1).savedPercent)
        assertEquals(100, Tally(5, 0).savedPercent)
    }

    @Test
    fun namesDaysAndUpsert() {
        assertEquals("Leftover chili", Food.cleanName("  Leftover   chili ", "Food"))
        assertEquals("Pizza", Food.cleanName("   ", "Pizza"))
        assertEquals(Food.MIN_DAYS, Food.clampDays(0))
        assertEquals(Food.MAX_DAYS, Food.clampDays(99))

        val a = item("a", "Soup", 0, 4)
        val list = Food.upsert(emptyList(), a)
        val edited = Food.upsert(list, a.copy(name = "Chicken soup"))
        assertEquals(1, edited.size)
        assertEquals("Chicken soup", edited.single().name)
        assertEquals(emptyList<FoodItem>(), Food.remove(edited, "a"))
    }

    @Test
    fun presetsAreSensible() {
        assertTrue(Food.PRESETS.all { it.keepsForDays in Food.MIN_DAYS..Food.MAX_DAYS })
        assertEquals(Food.PRESETS.size, Food.PRESETS.map { it.name }.toSet().size)
        // Raw poultry/ground meat is the most perishable preset (USDA: 1-2 days).
        assertEquals(2, Food.PRESETS.minOf { it.keepsForDays })
    }
}
