package com.example.testing

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var loginButton: MaterialButton
    private lateinit var registerButton: MaterialButton
    private lateinit var logoImageView: ImageView
    private lateinit var loginCard: MaterialCardView
    private lateinit var particle1: View
    private lateinit var particle2: View
    private lateinit var particle3: View

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        databaseHelper = DatabaseHelper(this)
        sessionManager = SessionManager(this)

        setupViews()
        setupClickListeners()
    }

    private fun setupViews() {
        try {
            emailInput = findViewById(R.id.emailInput)
            passwordInput = findViewById(R.id.passwordInput)
            emailLayout = findViewById(R.id.emailLayout)
            passwordLayout = findViewById(R.id.passwordLayout)
            loginButton = findViewById(R.id.loginButton)
            registerButton = findViewById(R.id.registerButton)
            logoImageView = findViewById(R.id.logoImageView)
            loginCard = findViewById(R.id.loginCard)
            particle1 = findViewById(R.id.particle1)
            particle2 = findViewById(R.id.particle2)
            particle3 = findViewById(R.id.particle3)

            // Start animations
            startAnimations()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error setting up views: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            attemptLogin()
        }
        
        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        
        // Find and set up forgot password click listener
        findViewById<View>(R.id.forgotPasswordText)?.setOnClickListener {
            showResetPasswordDialog()
        }
    }

    private fun attemptLogin() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        // Clear previous errors
        emailLayout.error = null
        passwordLayout.error = null

        var hasError = false

        if (TextUtils.isEmpty(email)) {
            emailLayout.error = "Email is required"
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Please enter a valid email"
            hasError = true
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.error = "Password is required"
            hasError = true
        } else if (password.length < 6) {
            passwordLayout.error = "Password must be at least 6 characters"
            hasError = true
        }

        if (hasError) return

        // Show loading state
        loginButton.isEnabled = false
        loginButton.text = "Logging in..."

        // Attempt login
        val user = databaseHelper.loginUser(email, password)

        if (user != null) {
            // Login successful
            sessionManager.createLoginSession(user)
            Toast.makeText(this, "Welcome back, ${user.fullName}!", Toast.LENGTH_SHORT).show()

            // Navigate to main activity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            // Login failed
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            loginButton.isEnabled = true
            loginButton.text = "Login"
        }
    }

    override fun onBackPressed() {
        // Prevent going back to splash screen
        moveTaskToBack(true)
    }

    private fun startAnimations() {
        // Logo animation
        logoImageView.alpha = 0f
        logoImageView.translationY = -50f
        logoImageView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(1000)
            .start()

        // Login card animation
        loginCard.alpha = 0f
        loginCard.translationY = 50f
        loginCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(300)
            .start()

        // Particle animations
        startParticleAnimations()

        // Input field animations
        animateInputFields()
    }

    private fun startParticleAnimations() {
        // Particle 1
        particle1.animate()
            .translationY(-20f)
            .alpha(0.8f)
            .setDuration(2000)
            .withEndAction {
                particle1.animate()
                    .translationY(0f)
                    .alpha(0.6f)
                    .setDuration(2000)
                    .withEndAction { startParticleAnimations() }
                    .start()
            }
            .start()

        // Particle 2 (delayed)
        Handler(Looper.getMainLooper()).postDelayed({
            particle2.animate()
                .translationY(-15f)
                .alpha(0.6f)
                .setDuration(1800)
                .withEndAction {
                    particle2.animate()
                        .translationY(0f)
                        .alpha(0.4f)
                        .setDuration(1800)
                        .start()
                }
                .start()
        }, 1000)

        // Particle 3 (delayed)
        Handler(Looper.getMainLooper()).postDelayed({
            particle3.animate()
                .translationY(-25f)
                .alpha(0.7f)
                .setDuration(2200)
                .withEndAction {
                    particle3.animate()
                        .translationY(0f)
                        .alpha(0.5f)
                        .setDuration(2200)
                        .start()
                }
                .start()
        }, 2000)
    }

    private fun animateInputFields() {
        // Email field animation
        emailLayout.alpha = 0f
        emailLayout.translationX = -30f
        emailLayout.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(600)
            .setStartDelay(500)
            .start()

        // Password field animation
        passwordLayout.alpha = 0f
        passwordLayout.translationX = 30f
        passwordLayout.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(600)
            .setStartDelay(700)
            .start()

        // Login button animation
        loginButton.alpha = 0f
        loginButton.scaleX = 0.8f
        loginButton.scaleY = 0.8f
        loginButton.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setStartDelay(900)
            .start()
    }
    
    private fun showResetPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reset_password, null)
        
        val emailInput = dialogView.findViewById<TextInputEditText>(R.id.resetEmailInput)
        val newPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.newPasswordInput)
        val confirmPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.confirmPasswordInput)
        val emailLayout = dialogView.findViewById<TextInputLayout>(R.id.resetEmailLayout)
        val newPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.newPasswordLayout)
        val confirmPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.confirmPasswordLayout)
        val resetButton = dialogView.findViewById<MaterialButton>(R.id.resetButton)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.cancelButton)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Set dialog background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        resetButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()
            
            // Clear previous errors
            emailLayout.error = null
            newPasswordLayout.error = null
            confirmPasswordLayout.error = null
            
            var hasError = false
            
            if (TextUtils.isEmpty(email)) {
                emailLayout.error = "Email is required"
                hasError = true
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Please enter a valid email"
                hasError = true
            } else if (!databaseHelper.isEmailExists(email)) {
                emailLayout.error = "Email not found"
                hasError = true
            }
            
            if (TextUtils.isEmpty(newPassword)) {
                newPasswordLayout.error = "New password is required"
                hasError = true
            } else if (newPassword.length < 6) {
                newPasswordLayout.error = "Password must be at least 6 characters"
                hasError = true
            }
            
            if (TextUtils.isEmpty(confirmPassword)) {
                confirmPasswordLayout.error = "Please confirm your password"
                hasError = true
            } else if (newPassword != confirmPassword) {
                confirmPasswordLayout.error = "Passwords do not match"
                hasError = true
            }
            
            if (hasError) return@setOnClickListener
            
            // Show loading state
            resetButton.isEnabled = false
            resetButton.text = "Resetting..."
            
            // Attempt password reset
            val success = databaseHelper.resetPassword(email, newPassword)
            
            if (success) {
                Toast.makeText(this, "Password reset successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Failed to reset password. Please try again.", Toast.LENGTH_SHORT).show()
                resetButton.isEnabled = true
                resetButton.text = "Reset Password"
            }
        }
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
}
