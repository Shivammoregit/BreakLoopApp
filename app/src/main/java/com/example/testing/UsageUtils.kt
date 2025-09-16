package com.example.testing

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

object UsageUtils {

    private const val PREFS_NAME = "usage_stats_prefs"
    private const val KEY_LAST_RESET_DATE = "last_reset_date"
    private const val KEY_TODAY_TOTAL_MINUTES = "today_total_minutes"
    private const val PREFIX_DAILY_TOTAL = "daily_total_"
    private const val PREFIX_APP_USAGE = "usage_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    fun resetIfNeeded(context: Context) {
        val prefs = getPrefs(context)
        val lastResetDate = prefs.getString(KEY_LAST_RESET_DATE, null)
        val todayDate = getTodayDateString()

        if (lastResetDate != todayDate) {
            val editor = prefs.edit()

            // Save yesterday's total
            val yesterdayMinutes = prefs.getInt(KEY_TODAY_TOTAL_MINUTES, 0)
            val yesterdayDate = getYesterdayDateString()
            editor.putInt("$PREFIX_DAILY_TOTAL$yesterdayDate", yesterdayMinutes)

            // Reset today's total and app-specific usage
            editor.putInt(KEY_TODAY_TOTAL_MINUTES, 0)
            val allEntries = prefs.all
            for ((key, _) in allEntries) {
                if (key.startsWith(PREFIX_APP_USAGE)) {
                    editor.remove(key)
                }
            }

            editor.putString(KEY_LAST_RESET_DATE, todayDate)
            editor.apply()
        }
    }

    fun saveTodayTotalMinutes(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_TODAY_TOTAL_MINUTES, minutes).apply()
    }

    fun getTodayTotalMinutes(context: Context): Int {
        resetIfNeeded(context) // Ensure data is for today
        return getPrefs(context).getInt(KEY_TODAY_TOTAL_MINUTES, 0)
    }

    fun incrementUsageSeconds(context: Context, packageName: String, seconds: Int) {
        val prefs = getPrefs(context)
        val key = "$PREFIX_APP_USAGE$packageName"
        val currentSeconds = prefs.getInt(key, 0)
        prefs.edit().putInt(key, currentSeconds + seconds).apply()
    }

    fun getAppUsageMinutes(context: Context, packageName: String): Int {
        resetIfNeeded(context) // Ensure data is for today
        val key = "$PREFIX_APP_USAGE$packageName"
        val totalSeconds = getPrefs(context).getInt(key, 0)
        return totalSeconds / 60
    }

    fun getLastNDaysTotals(context: Context, days: Int): List<Pair<String, Int>> {
        resetIfNeeded(context)
        val prefs = getPrefs(context)
        val result = mutableListOf<Pair<String, Int>>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        for (i in 0 until days) {
            val dateString = sdf.format(calendar.time)
            val minutes = if (i == 0) {
                prefs.getInt(KEY_TODAY_TOTAL_MINUTES, 0)
            } else {
                prefs.getInt("$PREFIX_DAILY_TOTAL$dateString", 0)
            }
            result.add(dateString to minutes)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return result.reversed()
    }
}