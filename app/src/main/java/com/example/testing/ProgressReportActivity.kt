package com.example.testing

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ProgressReportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress_report)

        renderDayView()

        findViewById<View>(R.id.tabDay).setOnClickListener { renderDayView() }
        findViewById<View>(R.id.tabWeek).setOnClickListener { renderWeekView() }
        findViewById<View>(R.id.tabMonth).setOnClickListener { renderMonthView() }
    }

    private fun renderDayView() {
        val container = findViewById<LinearLayout>(R.id.reportContainer)
        container.removeAllViews()
        val last30 = UsageUtils.getLastNDaysTotals(this, 30)
        addBarList(container, last30)
        setActiveTab(R.id.tabDay)
    }

    private fun renderWeekView() {
        val container = findViewById<LinearLayout>(R.id.reportContainer)
        container.removeAllViews()
        val totals = UsageUtils.getAllDailyTotals(this)
        val weekly = totals.entries
            .sortedBy { it.key }
            .groupBy { it.key.substring(0, 7) } // yyyy-MM
            .flatMap { (_, monthEntries) ->
                monthEntries
                    .chunked(7)
                    .mapIndexed { index, chunk ->
                        val label = if (chunk.isNotEmpty()) chunk.first().key.substring(5) + "~" + chunk.last().key.substring(5) else "Week ${index + 1}"
                        label to chunk.sumOf { it.value }
                    }
            }
        addBarList(container, weekly)
        setActiveTab(R.id.tabWeek)
    }

    private fun renderMonthView() {
        val container = findViewById<LinearLayout>(R.id.reportContainer)
        container.removeAllViews()
        val totals = UsageUtils.getAllDailyTotals(this)
        val monthly = totals.entries
            .groupBy { it.key.substring(0, 7) } // yyyy-MM
            .toSortedMap()
            .map { (month, entries) -> month to entries.sumOf { it.value } }
        addBarList(container, monthly)
        setActiveTab(R.id.tabMonth)
    }

    private fun addBarList(container: LinearLayout, data: List<Pair<String, Int>>) {
        if (data.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No data yet"
                setTextColor(ContextCompat.getColor(this@ProgressReportActivity, R.color.textSecondary))
                textSize = 14f
                gravity = Gravity.CENTER
            }
            container.addView(tv)
            return
        }

        val maxVal = (data.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
        data.forEach { (label, minutes) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }

            val labelText = TextView(this).apply {
                text = label
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@ProgressReportActivity, R.color.textPrimary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f)
                maxLines = 1
            }

            val barFill = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 10, minutes / maxVal.toFloat())
                setBackgroundColor(ContextCompat.getColor(this@ProgressReportActivity, R.color.colorPrimary))
            }
            val barContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.55f)
                addView(barFill)
            }

            val valueText = TextView(this).apply {
                val hours = minutes / 60
                val rem = minutes % 60
                text = if (hours > 0) "${hours}h ${rem}m" else "${rem}m"
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@ProgressReportActivity, R.color.textSecondary))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.2f)
            }

            row.addView(labelText)
            row.addView(barContainer)
            row.addView(valueText)
            container.addView(row)
        }
    }

    private fun setActiveTab(activeId: Int) {
        val ids = listOf(R.id.tabDay, R.id.tabWeek, R.id.tabMonth)
        ids.forEach { id ->
            val tv = findViewById<TextView>(id)
            val isActive = id == activeId
            tv.setTextColor(ContextCompat.getColor(this, if (isActive) R.color.colorPrimary else R.color.textSecondary))
        }
    }
}


