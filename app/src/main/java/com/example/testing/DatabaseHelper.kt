package com.example.testing

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "BreakLoopApp.db"
        private const val DATABASE_VERSION = 1
        
        // Users table
        private const val TABLE_USERS = "users"
        private const val COLUMN_ID = "id"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_FULL_NAME = "full_name"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_LAST_LOGIN = "last_login"
        private const val COLUMN_IS_ACTIVE = "is_active"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_USERNAME TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_FULL_NAME TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_LAST_LOGIN INTEGER DEFAULT 0,
                $COLUMN_IS_ACTIVE INTEGER DEFAULT 1
            )
        """.trimIndent()
        
        db.execSQL(createUsersTable)
        Log.d("DatabaseHelper", "Database created successfully")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // User authentication methods
    fun registerUser(user: User): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_USERNAME, user.username)
            put(COLUMN_PASSWORD, user.password) // In production, hash this password
            put(COLUMN_FULL_NAME, user.fullName)
            put(COLUMN_CREATED_AT, user.createdAt)
            put(COLUMN_LAST_LOGIN, user.lastLogin)
            put(COLUMN_IS_ACTIVE, if (user.isActive) 1 else 0)
        }
        
        val result = db.insert(TABLE_USERS, null, values)
        db.close()
        return result
    }

    fun loginUser(email: String, password: String): User? {
        val db = readableDatabase
        val selection = "$COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ? AND $COLUMN_IS_ACTIVE = 1"
        val selectionArgs = arrayOf(email, password)
        
        val cursor = db.query(
            TABLE_USERS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME)),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                lastLogin = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_LOGIN)),
                isActive = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)) == 1
            )
            
            // Update last login time
            updateLastLogin(user.id)
        }
        
        cursor.close()
        db.close()
        return user
    }

    fun getUserById(id: Long): User? {
        val db = readableDatabase
        val selection = "$COLUMN_ID = ?"
        val selectionArgs = arrayOf(id.toString())
        
        val cursor = db.query(
            TABLE_USERS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME)),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                lastLogin = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_LOGIN)),
                isActive = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)) == 1
            )
        }
        
        cursor.close()
        db.close()
        return user
    }

    fun isEmailExists(email: String): Boolean {
        val db = readableDatabase
        val selection = "$COLUMN_EMAIL = ?"
        val selectionArgs = arrayOf(email)
        
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID),
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    fun isUsernameExists(username: String): Boolean {
        val db = readableDatabase
        val selection = "$COLUMN_USERNAME = ?"
        val selectionArgs = arrayOf(username)
        
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID),
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    private fun updateLastLogin(userId: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LAST_LOGIN, System.currentTimeMillis())
        }
        
        val selection = "$COLUMN_ID = ?"
        val selectionArgs = arrayOf(userId.toString())
        
        db.update(TABLE_USERS, values, selection, selectionArgs)
        db.close()
    }

    fun updateUser(user: User): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_USERNAME, user.username)
            put(COLUMN_FULL_NAME, user.fullName)
            put(COLUMN_IS_ACTIVE, if (user.isActive) 1 else 0)
        }
        
        val selection = "$COLUMN_ID = ?"
        val selectionArgs = arrayOf(user.id.toString())
        
        val result = db.update(TABLE_USERS, values, selection, selectionArgs)
        db.close()
        return result > 0
    }
    
    fun resetPassword(email: String, newPassword: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PASSWORD, newPassword) // In production, hash this password
        }
        
        val selection = "$COLUMN_EMAIL = ?"
        val selectionArgs = arrayOf(email)
        
        val result = db.update(TABLE_USERS, values, selection, selectionArgs)
        db.close()
        return result > 0
    }
    
    fun getUserByEmail(email: String): User? {
        val db = readableDatabase
        val selection = "$COLUMN_EMAIL = ?"
        val selectionArgs = arrayOf(email)
        
        val cursor = db.query(
            TABLE_USERS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME)),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                lastLogin = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_LOGIN)),
                isActive = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)) == 1
            )
        }
        
        cursor.close()
        db.close()
        return user
    }
}
