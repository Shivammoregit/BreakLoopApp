package com.example.testing

import android.content.Context

object TimeLimitManager {
    private const val PREFS_NAME = "blocked_apps"
    private const val TIME_LIMITS_KEY = "time_limits"

    fun loadTimeLimits(context: Context): MutableMap<String, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val map = mutableMapOf<String, Int>()
        prefs.getString(TIME_LIMITS_KEY, null)?.split("|")?.forEach { entry ->
            val parts = entry.split(",")
            if (parts.size == 2) {
                map[parts[0]] = parts[1].toIntOrNull() ?: 0
            }
        }
        return map
    }

    fun saveTimeLimits(context: Context, limits: Map<String, Int>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val str = limits.entries.joinToString("|") { "${it.key},${it.value}" }
        prefs.edit().putString(TIME_LIMITS_KEY, str).apply()
    }
}