package com.georgewitte.stillgood.logic

import java.util.UUID

/** Something that went into the fridge. Dates are epoch days (LocalDate.toEpochDay()). */
data class FoodItem(
    val id: String,
    val name: String,
    val emoji: String,
    val addedEpochDay: Long,
    val keepsForDays: Int,
) {
    /** The last day it should be eaten. */
    val eatByEpochDay: Long get() = addedEpochDay + keepsForDays
}

enum class Urgency { FRESH, SOON, TODAY, PAST }

data class FoodStatus(val daysLeft: Long, val urgency: Urgency, val label: String)

/** Running totals, used for the "saved vs. tossed" line. */
data class Tally(val eaten: Int, val tossed: Int) {
    val total: Int get() = eaten + tossed

    /** Percent of finished items that were eaten rather than thrown out, or null before the first one. */
    val savedPercent: Int? get() = if (total == 0) null else Math.round(eaten * 100f / total)
}

/** A one-tap starting point. Days follow USDA FoodKeeper fridge guidance (40°F / 4°C or colder). */
data class Preset(val name: String, val emoji: String, val keepsForDays: Int)

object Food {
    const val MIN_DAYS = 1
    const val MAX_DAYS = 30
    const val MAX_NAME_LENGTH = 40

    val PRESETS = listOf(
        Preset("Leftovers", "🍲", 4),
        Preset("Pizza", "🍕", 4),
        Preset("Cooked rice or pasta", "🍝", 4),
        Preset("Cooked meat or poultry", "🍗", 4),
        Preset("Cooked fish", "🐟", 3),
        Preset("Soup or stew", "🥣", 4),
        Preset("Raw chicken or ground meat", "🥩", 2),
        Preset("Opened deli meat", "🥪", 4),
        Preset("Cut fruit or salad", "🥗", 3),
        Preset("Hard-boiled eggs", "🥚", 7),
        Preset("Opened milk", "🥛", 7),
        Preset("Something else", "🍱", 3),
    )

    fun newId(): String = UUID.randomUUID().toString()

    fun status(item: FoodItem, todayEpochDay: Long): FoodStatus {
        val daysLeft = item.eatByEpochDay - todayEpochDay
        return when {
            daysLeft < 0 -> FoodStatus(daysLeft, Urgency.PAST, "Past date")
            daysLeft == 0L -> FoodStatus(daysLeft, Urgency.TODAY, "Eat today")
            daysLeft == 1L -> FoodStatus(daysLeft, Urgency.SOON, "Tomorrow")
            else -> FoodStatus(daysLeft, Urgency.FRESH, "$daysLeft days")
        }
    }

    /** Soonest "eat by" first; ties broken by name so the order is stable. */
    fun sortedForDisplay(items: List<FoodItem>): List<FoodItem> =
        items.sortedWith(compareBy<FoodItem>({ it.eatByEpochDay }, { it.name.lowercase() }))

    /** How many items need eating today (or are already past their date). */
    fun needsAttention(items: List<FoodItem>, todayEpochDay: Long): Int =
        items.count { status(it, todayEpochDay).urgency.let { u -> u == Urgency.TODAY || u == Urgency.PAST } }

    fun clampDays(days: Int): Int = days.coerceIn(MIN_DAYS, MAX_DAYS)

    fun cleanName(raw: String, fallback: String): String =
        raw.trim().replace(Regex("\\s+"), " ").take(MAX_NAME_LENGTH).ifEmpty { fallback }

    fun upsert(items: List<FoodItem>, item: FoodItem): List<FoodItem> =
        if (items.any { it.id == item.id }) items.map { if (it.id == item.id) item else it } else items + item

    fun remove(items: List<FoodItem>, id: String): List<FoodItem> = items.filterNot { it.id == id }
}
