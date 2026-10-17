package com.georgewitte.stillgood

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.georgewitte.stillgood.data.FoodStore
import com.georgewitte.stillgood.logic.Food
import com.georgewitte.stillgood.logic.FoodItem
import com.georgewitte.stillgood.logic.Tally
import com.georgewitte.stillgood.logic.Urgency
import com.georgewitte.stillgood.ui.dp
import com.georgewitte.stillgood.ui.roundedRect
import com.georgewitte.stillgood.ui.selectableForeground
import com.georgewitte.stillgood.ui.setUpEdgeToEdge
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The fridge log: everything you put away, soonest "eat by" first. */
class MainActivity : Activity() {

    private lateinit var store: FoodStore
    private lateinit var listView: LinearLayout
    private lateinit var summaryView: TextView
    private lateinit var emptyView: View
    private lateinit var tallyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpEdgeToEdge(findViewById(R.id.root))
        store = FoodStore(this)
        listView = findViewById(R.id.food_list)
        summaryView = findViewById(R.id.summary)
        emptyView = findViewById(R.id.empty)
        tallyView = findViewById(R.id.tally)
        findViewById<View>(R.id.add_food).setOnClickListener { startActivity(AddFoodActivity.intent(this, null)) }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val today = LocalDate.now().toEpochDay()
        val items = Food.sortedForDisplay(store.loadItems())

        summaryView.text = if (items.isEmpty()) {
            getString(R.string.summary_empty)
        } else {
            val attention = Food.needsAttention(items, today)
            val count = resources.getQuantityString(R.plurals.item_count, items.size, items.size)
            if (attention == 0) {
                getString(R.string.summary_all_good, count)
            } else {
                getString(R.string.summary_attention, count, attention)
            }
        }
        emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

        listView.removeAllViews()
        for (item in items) {
            listView.addView(
                row(item, today),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = dp(10) },
            )
        }

        val tally = store.loadTally()
        val percent = tally.savedPercent
        tallyView.visibility = if (percent == null) View.GONE else View.VISIBLE
        if (percent != null) tallyView.text = getString(R.string.tally, tally.eaten, tally.tossed, percent)
    }

    private fun row(item: FoodItem, today: Long): View {
        val status = Food.status(item, today)
        val (strong, soft) = when (status.urgency) {
            Urgency.FRESH -> R.color.fresh to R.color.fresh_soft
            Urgency.SOON -> R.color.soon to R.color.soon_soft
            Urgency.TODAY -> R.color.today to R.color.today_soft
            Urgency.PAST -> R.color.past to R.color.past_soft
        }
        val dateFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
        val added = LocalDate.ofEpochDay(item.addedEpochDay).format(dateFormat)
        val eatBy = LocalDate.ofEpochDay(item.eatByEpochDay).format(dateFormat)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(76)
            setPadding(dp(12), dp(12), dp(14), dp(12))
            background = roundedRect(getColor(R.color.surface), dp(18).toFloat(), getColor(R.color.outline), dp(1))
            foreground = selectableForeground()
            isClickable = true
            isFocusable = true
            contentDescription = getString(R.string.row_description, item.name, status.label, eatBy)
            setOnClickListener { showActions(item) }
        }
        val stripe = View(this).apply { background = roundedRect(getColor(strong), dp(3).toFloat()) }
        row.addView(stripe, LinearLayout.LayoutParams(dp(5), LinearLayout.LayoutParams.MATCH_PARENT).apply {
            marginEnd = dp(12)
        })
        row.addView(TextView(this).apply {
            text = item.emoji
            textSize = 28f
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            marginEnd = dp(12)
        })

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
        column.addView(TextView(this).apply {
            text = item.name
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(getColor(R.color.text))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        })
        column.addView(TextView(this).apply {
            text = getString(R.string.row_detail, added, eatBy)
            textSize = 13f
            setTextColor(getColor(R.color.text_muted))
            setPadding(0, dp(2), 0, 0)
        })
        row.addView(column, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        row.addView(TextView(this).apply {
            text = status.label
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(getColor(strong))
            background = roundedRect(getColor(soft), dp(12).toFloat())
            setPadding(dp(10), dp(6), dp(10), dp(6))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            marginStart = dp(10)
        })
        return row
    }

    private fun showActions(item: FoodItem) {
        val actions = arrayOf(
            getString(R.string.action_ate),
            getString(R.string.action_tossed),
            getString(R.string.action_edit),
        )
        AlertDialog.Builder(this)
            .setTitle("${item.emoji}  ${item.name}")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> finishItem(item, eaten = true)
                    1 -> finishItem(item, eaten = false)
                    else -> startActivity(AddFoodActivity.intent(this, item.id))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun finishItem(item: FoodItem, eaten: Boolean) {
        store.saveItems(Food.remove(store.loadItems(), item.id))
        val tally = store.loadTally()
        store.saveTally(if (eaten) Tally(tally.eaten + 1, tally.tossed) else Tally(tally.eaten, tally.tossed + 1))
        Toast.makeText(this, if (eaten) R.string.toast_ate else R.string.toast_tossed, Toast.LENGTH_SHORT).show()
        render()
    }
}
