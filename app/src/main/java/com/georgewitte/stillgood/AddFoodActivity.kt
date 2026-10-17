package com.georgewitte.stillgood

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.georgewitte.stillgood.data.FoodStore
import com.georgewitte.stillgood.logic.Food
import com.georgewitte.stillgood.logic.FoodItem
import com.georgewitte.stillgood.logic.Preset
import com.georgewitte.stillgood.ui.dp
import com.georgewitte.stillgood.ui.roundedRect
import com.georgewitte.stillgood.ui.selectableForeground
import com.georgewitte.stillgood.ui.setUpEdgeToEdge
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Add a new item (pick a preset, adjust if needed, save) or edit an existing one. */
class AddFoodActivity : Activity() {

    private lateinit var store: FoodStore
    private lateinit var nameInput: EditText
    private lateinit var presetGrid: GridLayout
    private lateinit var daysView: TextView
    private lateinit var addedRow: LinearLayout
    private lateinit var eatByView: TextView

    private var editingId: String? = null
    private var preset: Preset? = null
    private var emoji = Food.PRESETS.first().emoji
    private var keepsForDays = Food.PRESETS.first().keepsForDays
    private val today: Long = LocalDate.now().toEpochDay()
    private var addedEpochDay = today
    private var originalAddedEpochDay: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_food)
        setUpEdgeToEdge(findViewById(R.id.root))
        store = FoodStore(this)

        nameInput = findViewById(R.id.name)
        presetGrid = findViewById(R.id.preset_grid)
        daysView = findViewById(R.id.days)
        addedRow = findViewById(R.id.added_row)
        eatByView = findViewById(R.id.eat_by)

        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.days_minus).setOnClickListener { changeDays(-1) }
        findViewById<View>(R.id.days_plus).setOnClickListener { changeDays(+1) }
        findViewById<View>(R.id.save).setOnClickListener { save() }

        editingId = intent.getStringExtra(EXTRA_ID)
        val existing = editingId?.let { id -> store.loadItems().firstOrNull { it.id == id } }
        if (editingId != null && existing == null) {
            finish()
            return
        }

        if (existing != null) {
            findViewById<TextView>(R.id.title).setText(R.string.edit_title)
            findViewById<TextView>(R.id.save).setText(R.string.save_changes)
            findViewById<View>(R.id.preset_section).visibility = View.GONE
            nameInput.setText(existing.name)
            emoji = existing.emoji
            keepsForDays = existing.keepsForDays
            addedEpochDay = existing.addedEpochDay
            originalAddedEpochDay = existing.addedEpochDay
        } else {
            buildPresets()
            selectPreset(Food.PRESETS.first())
        }

        buildAddedChoices()
        renderDays()
    }

    private fun buildPresets() {
        presetGrid.removeAllViews()
        for (p in Food.PRESETS) {
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                foreground = selectableForeground()
                isClickable = true
                isFocusable = true
                tag = p
                contentDescription = getString(R.string.preset_description, p.name, p.keepsForDays)
                setOnClickListener { selectPreset(p) }
            }
            tile.addView(TextView(this).apply {
                text = p.emoji
                textSize = 26f
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            tile.addView(TextView(this).apply {
                text = p.name
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(getColor(R.color.text))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            tile.addView(TextView(this).apply {
                text = resources.getQuantityString(R.plurals.days, p.keepsForDays, p.keepsForDays)
                textSize = 12f
                setTextColor(getColor(R.color.text_muted))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            val params = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
            ).apply {
                width = 0
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            presetGrid.addView(tile, params)
        }
    }

    private fun selectPreset(p: Preset) {
        // Only replace the name if the user hasn't typed their own.
        val typed = nameInput.text.toString().trim()
        if (typed.isEmpty() || Food.PRESETS.any { it.name == typed }) nameInput.setText(p.name)
        preset = p
        emoji = p.emoji
        keepsForDays = p.keepsForDays
        for (i in 0 until presetGrid.childCount) {
            val tile = presetGrid.getChildAt(i)
            val selected = tile.tag == p
            tile.isSelected = selected
            tile.background = if (selected) {
                roundedRect(getColor(R.color.brand_soft), dp(16).toFloat(), getColor(R.color.brand), dp(2))
            } else {
                roundedRect(getColor(R.color.surface), dp(16).toFloat(), getColor(R.color.outline), dp(1))
            }
        }
        renderDays()
    }

    private fun changeDays(delta: Int) {
        keepsForDays = Food.clampDays(keepsForDays + delta)
        renderDays()
    }

    private fun renderDays() {
        daysView.text = resources.getQuantityString(R.plurals.days, keepsForDays, keepsForDays)
        updateEatBy()
    }

    private fun buildAddedChoices() {
        addedRow.removeAllViews()
        val choices = mutableListOf(
            today to getString(R.string.added_today),
            today - 1 to getString(R.string.added_yesterday),
            today - 2 to getString(R.string.added_two_days),
        )
        val original = originalAddedEpochDay
        if (original != null && choices.none { it.first == original }) {
            val label = LocalDate.ofEpochDay(original).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
            choices.add(original to label)
        }
        for ((day, label) in choices) {
            val pill = TextView(this).apply {
                text = label
                textSize = 14f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                tag = day
                isClickable = true
                isFocusable = true
                setPadding(dp(14), 0, dp(14), 0)
                setOnClickListener {
                    addedEpochDay = day
                    renderAddedChoices()
                    updateEatBy()
                }
            }
            addedRow.addView(pill, LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                marginEnd = dp(8)
            })
        }
        renderAddedChoices()
    }

    private fun renderAddedChoices() {
        for (i in 0 until addedRow.childCount) {
            val pill = addedRow.getChildAt(i) as TextView
            val selected = pill.tag == addedEpochDay
            pill.isSelected = selected
            pill.background = if (selected) {
                roundedRect(getColor(R.color.brand), dp(22).toFloat())
            } else {
                roundedRect(getColor(R.color.surface), dp(22).toFloat(), getColor(R.color.outline), dp(1))
            }
            pill.setTextColor(getColor(if (selected) R.color.on_brand else R.color.text))
        }
    }

    private fun updateEatBy() {
        val eatBy = LocalDate.ofEpochDay(addedEpochDay + keepsForDays)
        val formatted = eatBy.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
        eatByView.text = getString(R.string.eat_by_preview, formatted)
    }

    private fun save() {
        val fallback = preset?.name ?: getString(R.string.default_food_name)
        val item = FoodItem(
            id = editingId ?: Food.newId(),
            name = Food.cleanName(nameInput.text.toString(), fallback),
            emoji = emoji,
            addedEpochDay = addedEpochDay,
            keepsForDays = Food.clampDays(keepsForDays),
        )
        store.saveItems(Food.upsert(store.loadItems(), item))
        finish()
    }

    companion object {
        private const val EXTRA_ID = "item_id"

        fun intent(context: Context, itemId: String?): Intent =
            Intent(context, AddFoodActivity::class.java).apply { if (itemId != null) putExtra(EXTRA_ID, itemId) }
    }
}
