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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import android.view.LayoutInflater
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1002
    }

    private lateinit var screenTimeProgress: CircularProgressIndicator
    private lateinit var totalScreenTimeTextView: TextView
    private lateinit var motivationalQuoteTextView: TextView
    private lateinit var nextButton: MaterialButton
    private lateinit var usageGraphContainer: LinearLayout
    private lateinit var miniBarChart: BarChart
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar

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
        miniBarChart = findViewById(R.id.miniBarChart)
        drawerLayout = findViewById(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbar)
        navigationView = findViewById(R.id.nav_view)
    }

    private var periodicUpdateHandler: android.os.Handler? = null
    private var periodicUpdateRunnable: Runnable? = null

    override fun onResume() {
        super.onResume()
        // Check for the essential permission first
        if (!hasUsageStatsPermission(this)) {
            showUsagePermissionDialog()
        } else {
            // Permissions are granted, proceed with normal setup
            setupUI()
            updateStatsDisplay()
            startPeriodicStatsUpdate()
        }
    }

    override fun onPause() {
        super.onPause()
        stopPeriodicStatsUpdate()
    }

    private fun setupUI() {
        // Setup Toolbar
        setSupportActionBar(toolbar)

        // Setup Navigation Drawer
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set the checked item
        navigationView.setCheckedItem(R.id.nav_dashboard)

        // Update nav header with user info
        updateNavHeader()

        nextButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, ProgressReportActivity::class.java))
        }

        updateStatsDisplay()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                // Already here, just close the drawer
            }
            R.id.nav_journal -> {
                startActivity(Intent(this, NotesActivity::class.java))
            }
            R.id.nav_scheduling -> {
                startActivity(Intent(this, SchedulingActivity::class.java))
            }
            R.id.nav_select_apps -> {
                if (hasUsageStatsPermission(this)) {
                    startActivity(Intent(this, AppSelectionActivity::class.java))
                } else {
                    Toast.makeText(this, "Please grant usage access first", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.nav_time_limits -> {
                val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
                val hasSelectedApps = (prefs.getStringSet("blocked_packages", emptySet())?.isNotEmpty() == true)
                if (hasSelectedApps) {
                    startActivity(Intent(this, TimeLimitActivity::class.java))
                } else {
                    Toast.makeText(this, "Please select apps first", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.nav_progress_report -> {
                startActivity(Intent(this, ProgressReportActivity::class.java))
            }
            R.id.nav_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
            }
            R.id.nav_logout -> {
                showLogoutConfirmation()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
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
            val progressPercentage = if (maxMinutesInDay > 0) (totalDeviceMinutes.toFloat() / maxMinutesInDay * 100).toInt() else 0
            screenTimeProgress.progress = progressPercentage


            val topAppsUsage = getTopAppsUsage()
            val weeklyProgress = getWeeklyProgressData()

            totalScreenTimeTextView.text = "${totalDeviceHours}h ${totalDeviceRemainingMinutes}m"
            UsageUtils.saveTodayTotalMinutes(this, totalDeviceMinutes)

            updateUsageGraph(topAppsUsage, totalDeviceMinutes)
            setupMiniBarChart(weeklyProgress)

            val quotes = resources.getStringArray(R.array.motivational_quotes)
            motivationalQuoteTextView.text = quotes.random()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateNavHeader() {
        val headerView = navigationView.getHeaderView(0)
        val nameTextView = headerView.findViewById<TextView>(R.id.navHeaderName)
        val emailTextView = headerView.findViewById<TextView>(R.id.navHeaderEmail)
        val sessionManager = SessionManager(this)
        val currentUser = sessionManager.getCurrentUser()
        nameTextView.text = currentUser?.fullName ?: "Guest"
        emailTextView.text = currentUser?.email ?: ""
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

    private val ignoredPackagesPrefixes = listOf(
        "com.android", "android", "com.google.android", "com.miui", "com.samsung.android"
    )
    private val ignoredPackagesKeywords = listOf(
        "system", "service", "launcher", "home", "settings", "permission", "manager"
    )

    private fun isPackageIgnored(packageName: String): Boolean {
        if (packageName.startsWith(this.packageName)) return true
        if (ignoredPackagesPrefixes.any { packageName.startsWith(it) }) return true
        if (ignoredPackagesKeywords.any { packageName.contains(it) }) return true
        return false
    }

    private fun isAppPackage(usageStat: android.app.usage.UsageStats): Boolean {
        return usageStat.totalTimeInForeground > 0 && !isPackageIgnored(usageStat.packageName)
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
                if (isAppPackage(usageStat)) {
                    totalMinutes += (usageStat.totalTimeInForeground / (1000 * 60)).toInt()
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
                if (isAppPackage(usageStat)) {
                    val minutes = (usageStat.totalTimeInForeground / (1000 * 60)).toInt()
                    if (minutes > 0) {
                        appUsageMap[usageStat.packageName] = minutes
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

        val inflater = LayoutInflater.from(this)
        topAppsUsage.forEach { (pkg, minutes) ->
            val percentage = if (totalDeviceMinutes > 0) (minutes.toFloat() / totalDeviceMinutes * 100).toInt() else 0
            val appName = getAppName(pkg)

            val graphRow = inflater.inflate(R.layout.item_usage_graph_row, usageGraphContainer, false)

            val nameText = graphRow.findViewById<TextView>(R.id.appNameText)
            val progressFill = graphRow.findViewById<View>(R.id.progressFill)
            val progressBackground = graphRow.findViewById<View>(R.id.progressBackground)
            val timeText = graphRow.findViewById<TextView>(R.id.timeText)

            nameText.text = appName
            timeText.text = "${minutes}m (${percentage}%)"

            (progressFill.layoutParams as LinearLayout.LayoutParams).weight = percentage.toFloat()
            (progressBackground.layoutParams as LinearLayout.LayoutParams).weight = 100f - percentage.toFloat()

            usageGraphContainer.addView(graphRow)
        }
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
    
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }


    private fun showProfileOptions() {
        val sessionManager = SessionManager(this)
        val currentUser = sessionManager.getCurrentUser()
        
        val options = arrayOf("View Profile", "Logout")
        
        AlertDialog.Builder(this)
            .setTitle("Profile Options")
            .setMessage("Welcome, ${currentUser?.fullName ?: "User"}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // View Profile
                        startActivity(Intent(this, ProfileActivity::class.java))
                    }
                    1 -> {
                        // Logout
                        showLogoutConfirmation()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun logout() {
        val sessionManager = SessionManager(this)
        sessionManager.logout()
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        
        // Navigate to login screen
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showUsagePermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app's core feature requires Usage Access to monitor app screen time. Please grant this permission in settings to continue.")
            .setPositiveButton("Go to Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            .setNegativeButton("Exit App") { _, _ ->
                // Exit the app if the user refuses
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
