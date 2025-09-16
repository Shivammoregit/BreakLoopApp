package com.example.testing

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 3000 // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Set a random quote
        setRandomQuote()

        // Start animations
        startAnimations()

        // Navigate to the next screen after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user is logged in
            val sessionManager = SessionManager(this)
            if (sessionManager.isLoggedIn()) {
                // User is logged in, go to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // User is not logged in, go to LoginActivity
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish() // Finish SplashActivity so user can't go back to it
        }, SPLASH_DELAY)
    }

    private fun startAnimations() {
        try {
            val logoImageView: ImageView = findViewById(R.id.logoImageView)
            val appNameTextView: TextView = findViewById(R.id.appNameTextView)
            val quoteTextView: TextView = findViewById(R.id.splashQuoteTextView)
            val particle1: View = findViewById(R.id.particle1)
            val particle2: View = findViewById(R.id.particle2)
            val particle3: View = findViewById(R.id.particle3)

            // Fade-in and slide-up for logo and quote
            logoImageView.alpha = 0f
            appNameTextView.alpha = 0f
            quoteTextView.alpha = 0f
            logoImageView.translationY = 100f
            appNameTextView.translationY = 100f
            quoteTextView.translationY = 100f

            logoImageView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1000)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setStartDelay(500)
                .start()

            appNameTextView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1000)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setStartDelay(600)
                .start()

            quoteTextView.animate()
                .alpha(0.9f)
                .translationY(0f)
                .setDuration(1000)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setStartDelay(800)
                .start()

            // Background particle animations
            animateParticle(particle1, 20000, 300f)
            animateParticle(particle2, 25000, -400f)
            animateParticle(particle3, 18000, 250f)
        } catch (e: Exception) {
            // Don't crash the app if animations fail
            e.printStackTrace()
        }
    }

    private fun setRandomQuote() {
        try {
            val quoteTextView: TextView = findViewById(R.id.splashQuoteTextView)

            // The splash quotes are named splash_quote_1, splash_quote_2, ..., splash_quote_10
            val quoteCount = 10
            val randomQuoteIndex = Random.nextInt(1, quoteCount + 1)
            val resourceName = "splash_quote_$randomQuoteIndex"
            
            val resourceId = resources.getIdentifier(resourceName, "string", packageName)

            if (resourceId != 0) {
                quoteTextView.text = getString(resourceId)
            } else {
                quoteTextView.text = getString(R.string.app_description)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun animateParticle(view: View, duration: Long, translation: Float) {
        val animator = ObjectAnimator.ofFloat(view, "translationY", view.translationY, view.translationY + translation)
        animator.duration = duration
        animator.repeatCount = ObjectAnimator.INFINITE
        animator.repeatMode = ObjectAnimator.REVERSE
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }
}