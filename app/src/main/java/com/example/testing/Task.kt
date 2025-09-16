package com.example.testing

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String?,
    val isCompleted: Boolean = false,
    val dueAtMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)