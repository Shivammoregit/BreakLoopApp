package com.example.testing

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.app.usage.UsageStatsManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min
import android.view.Gravity

class MainActivity : AppCompatActivity() {
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1002
    }

    private lateinit var screenTimeProgress: CircularProgressIndicator
    private lateinit var totalScreenTimeTextView: TextView
    private lateinit var motivationalQuoteTextView: TextView
    private lateinit var nextButton: MaterialButton
    private lateinit var usageGraphContainer: LinearLayout
    private lateinit var profileIconContainer: LinearLayout
    private lateinit var miniBarChart: BarChart
    private lateinit var selectAppsButton: MaterialButton
    private lateinit var setTimeLimitsButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            setupViews()
            setupSwipeRefresh()
            setupUI()
            startBlockerServiceIfNeeded()
            startPeriodicStatsUpdate()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error starting app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupViews() {
        screenTimeProgress = findViewById(R.id.screenTimeProgress)
        totalScreenTimeTextView = findViewById(R.id.totalScreenTime)
        motivationalQuoteTextView = findViewById(R.id.motivationalQuote)
        nextButton = findViewById(R.id.nextButton)
        usageGraphContainer = findViewById(R.id.usageGraphContainer)
        profileIconContainer = findViewById(R.id.profileIconContainer)
        miniBarChart = findViewById(R.id.miniBarChart)
        selectAppsButton = findViewById(R.id.selectAppsButton)
        setTimeLimitsButton = findViewById(R.id.setTimeLimitsButton)
    }

    private var periodicUpdateHandler: android.os.Handler? = null
    private var periodicUpdateRunnable: Runnable? = null

    override fun onResume() {
        super.onResume()
        setupUI()
        updateStatsDisplay()
        startPeriodicStatsUpdate()
    }

    override fun onPause() {
        super.onPause()
        stopPeriodicStatsUpdate()
    }

    private fun setupUI() {
        nextButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, ProgressReportActivity::class.java))
        }

        profileIconContainer.setOnClickListener {
            startActivity(Intent(this@MainActivity, ProfileActivity::class.java))
        }

        selectAppsButton.setOnClickListener {
            if (hasUsageStatsPermission(this@MainActivity)) {
                startActivity(Intent(this@MainActivity, AppSelectionActivity::class.java))
            } else {
                Toast.makeText(this, "Please grant usage access first", Toast.LENGTH_SHORT).show()
            }
        }

        setTimeLimitsButton.setOnClickListener {
            val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
            val hasSelectedApps = (prefs.getStringSet("blocked_packages", emptySet())?.isNotEmpty() == true)

            if (hasSelectedApps) {
                startActivity(Intent(this@MainActivity, TimeLimitBlockingActivity::class.java))
            } else {
                Toast.makeText(this, "Please select apps first", Toast.LENGTH_SHORT).show()
            }
        }
        updateStatsDisplay()
    }

    private fun startBlockerServiceIfNeeded() {
        try {
            val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
            val hasSelectedApps = (prefs.getStringSet("blocked_packages", emptySet())?.isNotEmpty() == true)
            val hasTimeLimits = !prefs.getString("time_limits", null).isNullOrEmpty()

            if (hasUsageStatsPermission(this) && hasSelectedApps && hasTimeLimits) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, BlockerService::class.java))
                        Toast.makeText(this@MainActivity, "Monitoring service started", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Failed to start monitoring service: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }, 500)
            } else {
                try {
                    stopService(Intent(this, BlockerService::class.java))
                } catch (e: Exception) {
                    // Ignore errors when stopping service
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error managing service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                context.applicationInfo.uid,
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Binder.getCallingUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun setupSwipeRefresh() {
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            UsageUtils.resetIfNeeded(this)
            updateStatsDisplay()
            swipeRefreshLayout.isRefreshing = false
            Toast.makeText(this, "Stats refreshed!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatsDisplay() {
        try {
            UsageUtils.resetIfNeeded(this)
            val totalDeviceMinutes = getTotalDeviceScreenTime()
            val totalDeviceHours = totalDeviceMinutes / 60
            val totalDeviceRemainingMinutes = totalDeviceMinutes % 60

            val maxMinutesInDay = 24 * 60
            val progressPercentage = (totalDeviceMinutes.toFloat() / maxMinutesInDay * 100).toInt()
            screenTimeProgress.progress = progressPercentage

            val topAppsUsage = getTopAppsUsage()
            val weeklyProgress = getWeeklyProgressData()

            totalScreenTimeTextView.text = "${totalDeviceHours}h ${totalDeviceRemainingMinutes}m"
            UsageUtils.saveTodayTotalMinutes(this, totalDeviceMinutes)

            updateUsageGraph(topAppsUsage, totalDeviceMinutes)
            setupMiniBarChart(weeklyProgress)

            val quotes = listOf(
                "The only way to do great work is to love what you do.",
                "Believe you can and you're halfway there.",
                "The future belongs to those who believe in the beauty of their dreams.",
                "Strive not to be a success, but rather to be of value.",
                "The best way to predict the future is to create it."
            )
            motivationalQuoteTextView.text = quotes.random()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getWeeklyProgressData(): List<Int> {
        val totalMinutesInDay = 24 * 60
        val last7DaysUsage = UsageUtils.getLastNDaysTotals(this, 7).map { it.second }
        return last7DaysUsage.map {
            totalMinutesInDay - min(it, totalMinutesInDay)
        }.reversed()
    }

    private fun setupMiniBarChart(data: List<Int>) {
        val entries = data.mapIndexed { index, progressValue -> BarEntry(index.toFloat(), progressValue.toFloat()) }
        val dataSet = BarDataSet(entries, "Weekly Progress").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.colorSecondary)
            setDrawValues(false)
        }
        val barData = BarData(dataSet)
        miniBarChart.data = barData
        miniBarChart.description.isEnabled = false
        miniBarChart.setTouchEnabled(false)
        miniBarChart.setDrawGridBackground(false)
        miniBarChart.legend.isEnabled = false

        val xAxis = miniBarChart.xAxis
        val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        xAxis.valueFormatter = IndexAxisValueFormatter(days.copyOfRange(0, data.size))
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.textColor = ContextCompat.getColor(this, R.color.textSecondary)

        miniBarChart.axisRight.isEnabled = false
        miniBarChart.axisLeft.isEnabled = false

        miniBarChart.invalidate()
    }

    private fun startPeriodicStatsUpdate() {
        stopPeriodicStatsUpdate()

        periodicUpdateHandler = android.os.Handler(android.os.Looper.getMainLooper())
        periodicUpdateRunnable = object : Runnable {
            override fun run() {
                if (!isFinishing) {
                    updateStatsDisplay()
                    periodicUpdateHandler?.postDelayed(this, 30000)
                }
            }
        }

        periodicUpdateRunnable?.let { runnable ->
            periodicUpdateHandler?.postDelayed(runnable, 30000)
        }
    }

    private fun stopPeriodicStatsUpdate() {
        periodicUpdateRunnable?.let { runnable ->
            periodicUpdateHandler?.removeCallbacks(runnable)
        }
        periodicUpdateHandler = null
        periodicUpdateRunnable = null
    }

    private fun getTotalDeviceScreenTime(): Int {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            )

            var totalMinutes = 0
            for (usageStat in usageStats) {
                val packageName = usageStat.packageName
                if (usageStat.totalTimeInForeground > 0 &&
                    !packageName.startsWith("com.android") &&
                    !packageName.startsWith("android") &&
                    !packageName.startsWith("com.google.android") &&
                    !packageName.startsWith(this.packageName) &&
                    !packageName.contains("system") &&
                    !packageName.contains("service") &&
                    !packageName.contains("launcher") &&
                    !packageName.contains("home") &&
                    !packageName.contains("settings") &&
                    !packageName.contains("permission") &&
                    !packageName.contains("manager")) {

                    val minutes = (usageStat.totalTimeInForeground / (1000 * 60)).toInt()
                    totalMinutes += minutes
                }
            }
            val maxPossibleMinutes = ((endTime - startTime) / (1000 * 60)).toInt()
            min(totalMinutes, maxPossibleMinutes)
        } catch (e: Exception) {
            0
        }
    }

    private fun getTopAppsUsage(): List<Pair<String, Int>> {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            )

            val appUsageMap = mutableMapOf<String, Int>()

            for (usageStat in usageStats) {
                val packageName = usageStat.packageName
                val timeInForeground = usageStat.totalTimeInForeground

                if (timeInForeground > 0 &&
                    !packageName.startsWith("com.android") &&
                    !packageName.startsWith("android") &&
                    !packageName.startsWith("com.google.android") &&
                    !packageName.startsWith(this.packageName)) {

                    val minutes = (timeInForeground / (1000 * 60)).toInt()
                    if (minutes > 0) {
                        appUsageMap[packageName] = minutes
                    }
                }
            }

            val appUsage = appUsageMap.map { it.key to it.value }
            appUsage.sortedByDescending { it.second }.take(5)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun updateUsageGraph(topAppsUsage: List<Pair<String, Int>>, totalDeviceMinutes: Int) {
        usageGraphContainer.removeAllViews()

        if (totalDeviceMinutes == 0) {
            val noDataText = TextView(this).apply {
                text = "No usage data available"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.textSecondary))
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 16)
            }
            usageGraphContainer.addView(noDataText)
            return
        }

        topAppsUsage.forEach { (pkg, minutes) ->
            val percentage = (minutes.toFloat() / totalDeviceMinutes * 100).toInt()
            val appName = getAppName(pkg)

            val graphRow = createGraphRow(appName, minutes, percentage)
            usageGraphContainer.addView(graphRow)
        }
    }

    private fun createGraphRow(appName: String, minutes: Int, percentage: Int): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }

        val nameText = TextView(this).apply {
            text = appName
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.textPrimary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f)
            maxLines = 1
        }

        val progressContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f)
        }

        val progressFill = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                8,
                percentage.toFloat()
            )
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.colorPrimary))
        }

        val progressBackground = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                8,
                100f - percentage.toFloat()
            )
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.textSecondary))
            alpha = 0.3f
        }

        progressContainer.addView(progressFill)
        progressContainer.addView(progressBackground)


        val timeText = TextView(this).apply {
            text = "${minutes}m (${percentage}%)"
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.textSecondary))
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.2f)
        }

        row.addView(nameText)
        row.addView(progressContainer)
        row.addView(timeText)

        return row
    }

    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
