package com.georgewitte.stillgood.data

import android.content.Context
import com.georgewitte.stillgood.logic.FoodItem
import com.georgewitte.stillgood.logic.Tally
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** The fridge log and the eaten/tossed tally, stored on the device in SharedPreferences. */
class FoodStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("stillgood", Context.MODE_PRIVATE)

    fun loadItems(): List<FoodItem> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                FoodItem(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    emoji = obj.optString("emoji", "🍱"),
                    addedEpochDay = obj.getLong("added"),
                    keepsForDays = obj.getInt("days"),
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    fun saveItems(items: List<FoodItem>) {
        val array = JSONArray()
        for (item in items) {
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("emoji", item.emoji)
                    .put("added", item.addedEpochDay)
                    .put("days", item.keepsForDays),
            )
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    fun loadTally(): Tally = Tally(prefs.getInt(KEY_EATEN, 0), prefs.getInt(KEY_TOSSED, 0))

    fun saveTally(tally: Tally) {
        prefs.edit().putInt(KEY_EATEN, tally.eaten).putInt(KEY_TOSSED, tally.tossed).apply()
    }

    private companion object {
        const val KEY_ITEMS = "items_v1"
        const val KEY_EATEN = "eaten"
        const val KEY_TOSSED = "tossed"
    }
}
