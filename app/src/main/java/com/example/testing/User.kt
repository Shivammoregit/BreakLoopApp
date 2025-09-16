package com.example.testing

data class User(
    val id: Long = 0,
    val email: String,
    val username: String,
    val password: String,
    val fullName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = 0,
    val isActive: Boolean = true
)

