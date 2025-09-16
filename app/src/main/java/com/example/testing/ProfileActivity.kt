package com.example.testing

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var profilePhoto: ImageView

    // Register a launcher for the image picker activity result
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Handle the selected image URI
        uri?.let {
            profilePhoto.setImageURI(it)
            // Here you would typically save the URI to SharedPreferences or a database
            // to persist the user's choice.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Get references to views
        profilePhoto = findViewById(R.id.profilePhoto)
        val userDetailsTextView: TextView = findViewById(R.id.userId)
        val appInstallDateTextView: TextView = findViewById(R.id.appInstallDate)
        val todayScreenTimeTextView: TextView = findViewById(R.id.todayScreenTime)
        val appVersionTextView: TextView = findViewById(R.id.appVersion)
        val logoutButton: Button = findViewById(R.id.logoutButton)
        val exitButton: Button = findViewById(R.id.exitButton)

        // Set up the user details
        val sessionManager = SessionManager(this)
        val currentUser = sessionManager.getCurrentUser()

        val name = currentUser?.fullName ?: "Guest User"
        val email = currentUser?.email ?: "No email provided"
        userDetailsTextView.text = "Name: $name\nEmail: $email"

        // Get and display the app install date
        val installDate = getAppInstallDate()
        appInstallDateTextView.text = "Installed on: $installDate"

        // Get and display today's screen time
        val todayMinutes = UsageUtils.getTodayTotalMinutes(this)
        val hours = todayMinutes / 60
        val minutes = todayMinutes % 60
        todayScreenTimeTextView.text = "Today's Screen Time: ${hours}h ${minutes}m"

        // Get and display app version
        appVersionTextView.text = "App Version: ${getAppVersion()}"

        profilePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }

        exitButton.setOnClickListener {
            // This will close all activities and exit the app.
            finishAffinity()
        }
    }

    private fun getAppInstallDate(): String {
        return try {
            val packageManager = packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val installTime = packageInfo.firstInstallTime
            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            dateFormat.format(Date(installTime))
        } catch (e: Exception) {
            "Not available"
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "N/A"
        }
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

        // Navigate to login screen and clear activity stack
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
