package com.example.testing

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var fullNameInput: TextInputEditText
    private lateinit var usernameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    
    private lateinit var fullNameLayout: TextInputLayout
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    
    private lateinit var registerButton: MaterialButton
    private lateinit var loginButton: MaterialButton
    
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        databaseHelper = DatabaseHelper(this)
        
        setupViews()
        setupClickListeners()
    }
    
    private fun setupViews() {
        fullNameInput = findViewById(R.id.fullNameInput)
        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        
        fullNameLayout = findViewById(R.id.fullNameLayout)
        usernameLayout = findViewById(R.id.usernameLayout)
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)
        
        registerButton = findViewById(R.id.registerButton)
        loginButton = findViewById(R.id.loginButton)
    }
    
    private fun setupClickListeners() {
        registerButton.setOnClickListener {
            attemptRegister()
        }
        
        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    
    private fun attemptRegister() {
        val fullName = fullNameInput.text.toString().trim()
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()
        
        // Clear previous errors
        clearErrors()
        
        var hasError = false
        
        if (TextUtils.isEmpty(fullName)) {
            fullNameLayout.error = "Full name is required"
            hasError = true
        }
        
        if (TextUtils.isEmpty(username)) {
            usernameLayout.error = "Username is required"
            hasError = true
        } else if (username.length < 3) {
            usernameLayout.error = "Username must be at least 3 characters"
            hasError = true
        } else if (databaseHelper.isUsernameExists(username)) {
            usernameLayout.error = "Username already exists"
            hasError = true
        }
        
        if (TextUtils.isEmpty(email)) {
            emailLayout.error = "Email is required"
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Please enter a valid email"
            hasError = true
        } else if (databaseHelper.isEmailExists(email)) {
            emailLayout.error = "Email already exists"
            hasError = true
        }
        
        if (TextUtils.isEmpty(password)) {
            passwordLayout.error = "Password is required"
            hasError = true
        } else if (password.length < 6) {
            passwordLayout.error = "Password must be at least 6 characters"
            hasError = true
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.error = "Please confirm your password"
            hasError = true
        } else if (password != confirmPassword) {
            confirmPasswordLayout.error = "Passwords do not match"
            hasError = true
        }
        
        if (hasError) return
        
        // Show loading state
        registerButton.isEnabled = false
        registerButton.text = "Creating Account..."
        
        // Create user
        val user = User(
            email = email,
            username = username,
            password = password, // In production, hash this password
            fullName = fullName
        )
        
        val userId = databaseHelper.registerUser(user)
        
        if (userId != -1L) {
            // Registration successful
            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
            
            // Navigate to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // Registration failed
            Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show()
            registerButton.isEnabled = true
            registerButton.text = "Create Account"
        }
    }
    
    private fun clearErrors() {
        fullNameLayout.error = null
        usernameLayout.error = null
        emailLayout.error = null
        passwordLayout.error = null
        confirmPasswordLayout.error = null
    }
    
    override fun onBackPressed() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

