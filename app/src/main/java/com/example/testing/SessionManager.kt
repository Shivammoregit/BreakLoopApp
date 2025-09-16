package com.example.testing

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()

    companion object {
        private const val PREFS_NAME = "BreakLoopSession"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_USERNAME = "user_username"
        private const val KEY_USER_FULL_NAME = "user_full_name"
        private const val KEY_LAST_LOGIN = "last_login"
    }

    fun createLoginSession(user: User) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putLong(KEY_USER_ID, user.id)
        editor.putString(KEY_USER_EMAIL, user.email)
        editor.putString(KEY_USER_USERNAME, user.username)
        editor.putString(KEY_USER_FULL_NAME, user.fullName)
        editor.putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
        editor.apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getCurrentUser(): User? {
        return if (isLoggedIn()) {
            User(
                id = prefs.getLong(KEY_USER_ID, 0),
                email = prefs.getString(KEY_USER_EMAIL, "") ?: "",
                username = prefs.getString(KEY_USER_USERNAME, "") ?: "",
                password = "", // Don't store password in session
                fullName = prefs.getString(KEY_USER_FULL_NAME, "") ?: "",
                lastLogin = prefs.getLong(KEY_LAST_LOGIN, 0)
            )
        } else {
            null
        }
    }

    fun logout() {
        editor.clear()
        editor.apply()
    }

    fun updateUserInfo(user: User) {
        editor.putString(KEY_USER_EMAIL, user.email)
        editor.putString(KEY_USER_USERNAME, user.username)
        editor.putString(KEY_USER_FULL_NAME, user.fullName)
        editor.apply()
    }
}

