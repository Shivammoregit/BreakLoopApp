package com.example.testing

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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

        // Get references to views
        profilePhoto = findViewById(R.id.profilePhoto)
        val userIdTextView: TextView = findViewById(R.id.userId)
        val appInstallDateTextView: TextView = findViewById(R.id.appInstallDate)
        val logoutButton: Button = findViewById(R.id.logoutButton)

        // Set up the user details
        val userId = "User ID: ${UUID.randomUUID().toString().substring(0, 8)}"
        userIdTextView.text = userId

        // Get and display the app install date
        val installDate = getAppInstallDate()
        appInstallDateTextView.text = "Installed on: $installDate"

        // Set up the click listener for the profile photo
        profilePhoto.setOnClickListener {
            // Launch the image picker
            pickImageLauncher.launch("image/*")
        }

        // Set up the logout button listener
        logoutButton.setOnClickListener {
            // Clear all activities and exit the app
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
}
