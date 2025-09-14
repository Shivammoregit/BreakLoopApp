package com.example.testing

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

class ProgressReportActivity : AppCompatActivity() {

    private lateinit var phoneUsageChart: LineChart
    private lateinit var progressChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress_report)

        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }

        phoneUsageChart = findViewById(R.id.phoneUsageChart)
        progressChart = findViewById(R.id.progressChart)

        setupCharts()
    }

    private fun setupCharts() {
        val weeklyData = UsageUtils.getLastNDaysTotals(this, 7)
        val usageMinutes = weeklyData.map { it.second.toFloat() }

        setupUsageChart(usageMinutes)
        setupProgressChart(usageMinutes)
    }

    private fun setupUsageChart(usageMinutes: List<Float>) {
        val entries = usageMinutes.mapIndexed { index, minutes ->
            Entry(index.toFloat(), minutes)
        }

        val dataSet = LineDataSet(entries, "Total Usage").apply {
            color = ContextCompat.getColor(this@ProgressReportActivity, R.color.colorPrimary)
            valueTextColor = ContextCompat.getColor(this@ProgressReportActivity, R.color.textSecondary)
            lineWidth = 3f
            setDrawCircles(true)
            setCircleColor(ContextCompat.getColor(this@ProgressReportActivity, R.color.colorPrimary))
            setDrawValues(true)
            fillAlpha = 100
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(this@ProgressReportActivity, R.drawable.screen_time_background)
        }

        val lineData = LineData(dataSet)
        phoneUsageChart.data = lineData
        phoneUsageChart.description.isEnabled = false
        phoneUsageChart.setTouchEnabled(true)
        phoneUsageChart.setDrawGridBackground(false)
        phoneUsageChart.legend.isEnabled = false

        val xAxis = phoneUsageChart.xAxis
        val days = getDaysOfWeek()
        xAxis.valueFormatter = IndexAxisValueFormatter(days)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.textColor = ContextCompat.getColor(this, R.color.textSecondary)
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true

        phoneUsageChart.axisRight.isEnabled = false
        val yAxisLeft = phoneUsageChart.axisLeft
        yAxisLeft.setDrawGridLines(false)
        yAxisLeft.setDrawAxisLine(false)
        yAxisLeft.textColor = ContextCompat.getColor(this, R.color.textSecondary)
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.valueFormatter = object : IndexAxisValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}m"
            }
        }
        phoneUsageChart.invalidate()
    }

    private fun setupProgressChart(usageMinutes: List<Float>) {
        val progressEntries = mutableListOf<Entry>()
        var previousDayUsage = 0f

        for (i in usageMinutes.indices) {
            val currentDayUsage = usageMinutes[i]
            val progress: Float
            if (i == 0) {
                // For the first day, we calculate progress based on the previous week's average
                val previousWeekTotal = UsageUtils.getLastNDaysTotals(this, 7).dropLast(1).sumOf { it.second.toLong() }
                val previousWeekAverage = if (previousWeekTotal > 0) previousWeekTotal / 6 else 0L
                val reduction = previousWeekAverage - currentDayUsage
                val dailyImprovement = if (previousWeekAverage > 0) (reduction.toFloat() / previousWeekAverage) * 100 else 0f
                progress = min(100f, dailyImprovement)
            } else {
                val reduction = previousDayUsage - currentDayUsage
                val dailyImprovement = if (previousDayUsage > 0) (reduction / previousDayUsage) * 100 else 0f
                progress = (progressEntries.last().y) + min(20f, dailyImprovement)
            }
            progressEntries.add(Entry(i.toFloat(), progress))
            previousDayUsage = currentDayUsage
        }

        val dataSet = LineDataSet(progressEntries, "Brain Enhancement").apply {
            color = ContextCompat.getColor(this@ProgressReportActivity, R.color.colorSecondary)
            valueTextColor = ContextCompat.getColor(this@ProgressReportActivity, R.color.textSecondary)
            lineWidth = 3f
            setDrawCircles(true)
            setCircleColor(ContextCompat.getColor(this@ProgressReportActivity, R.color.colorSecondary))
            setDrawValues(true)
            fillAlpha = 100
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(this@ProgressReportActivity, R.drawable.app_icon_gradient)
        }

        val lineData = LineData(dataSet)
        progressChart.data = lineData
        progressChart.description.isEnabled = false
        progressChart.setTouchEnabled(true)
        progressChart.setDrawGridBackground(false)
        progressChart.legend.isEnabled = false

        val xAxis = progressChart.xAxis
        val days = getDaysOfWeek()
        xAxis.valueFormatter = IndexAxisValueFormatter(days)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.textColor = ContextCompat.getColor(this, R.color.textSecondary)
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true

        progressChart.axisRight.isEnabled = false
        val yAxisLeft = progressChart.axisLeft
        yAxisLeft.setDrawGridLines(false)
        yAxisLeft.setDrawAxisLine(false)
        yAxisLeft.textColor = ContextCompat.getColor(this, R.color.textSecondary)
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.axisMaximum = 100f
        yAxisLeft.valueFormatter = object : IndexAxisValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}%"
            }
        }
        progressChart.invalidate()
    }

    private fun getDaysOfWeek(): List<String> {
        val calendar = Calendar.getInstance()
        val days = mutableListOf<String>()
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        for (i in 0 until 7) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            days.add(dateFormat.format(calendar.time))
        }
        days.reverse()
        days[days.size - 1] = "Today"
        return days
    }
}
