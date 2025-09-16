package com.example.testing

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.content.Intent
import android.text.TextWatcher

class TimeLimitActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var saveButton: Button
    private lateinit var adapter: TimeLimitAdapter
    private lateinit var appList: List<AppInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LayoutInflater.from(this).inflate(R.layout.activity_time_limit, null)
        setContentView(root)

        recyclerView = root.findViewById(R.id.recyclerView)
        saveButton = root.findViewById(R.id.saveButton)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val specificAppPackage = intent.getStringExtra("specific_app_package")
        if (specificAppPackage != null) {
            // Show only the specific app that was blocked
            appList = loadSpecificApp(specificAppPackage)
        } else {
            // Show all selected apps (normal flow)
            appList = loadSelectedApps()
        }
        
        val timeLimits = TimeLimitManager.loadTimeLimits(this)
        adapter = TimeLimitAdapter(appList, timeLimits)
        recyclerView.adapter = adapter

        saveButton.setOnClickListener {
            val limits = adapter.getTimeLimits()
            saveTimeLimits(limits)
            Toast.makeText(this, "Time limits saved", Toast.LENGTH_SHORT).show()
            
            // If this was triggered from a blocked app, redirect back to it
            if (specificAppPackage != null) {
                redirectToApp(specificAppPackage)
            } else {
                finish()
            }
        }
    }

    private fun loadSelectedApps(): List<AppInfo> {
        val prefs = getSharedPreferences("blocked_apps", Context.MODE_PRIVATE)
        val pkgs = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
        val pm = packageManager
        return pkgs.mapNotNull { pkg ->
            try {
                val app = pm.getApplicationInfo(pkg, 0)
                AppInfo(
                    app.loadLabel(pm).toString(),
                    app.packageName,
                    app.loadIcon(pm)
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.appName }
    }

    private fun saveTimeLimits(limits: Map<String, Int>) {
        TimeLimitManager.saveTimeLimits(this, limits)
    }

    private fun loadSpecificApp(packageName: String): List<AppInfo> {
        val pm = packageManager
        return try {
            val app = pm.getApplicationInfo(packageName, 0)
            listOf(AppInfo(
                app.loadLabel(pm).toString(),
                app.packageName,
                app.loadIcon(pm)
            ))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun redirectToApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            // If we can't launch the app, just finish this activity
        }
        finish()
    }
}

class TimeLimitAdapter(
    private val apps: List<AppInfo>,
    initialTimeLimits: Map<String, Int>
) : RecyclerView.Adapter<TimeLimitAdapter.ViewHolder>() {

    // Use a mutable map to hold the current state of the limits, initialized with provided values.
    private val limits = initialTimeLimits.toMutableMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_limit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.appName
        holder.appIcon.setImageDrawable(app.icon)
        val limit = limits[app.packageName] ?: 0
        holder.timeLimit.setText(if (limit > 0) limit.toString() else "")
        holder.timeLimit.inputType = InputType.TYPE_CLASS_NUMBER
        holder.timeLimit.hint = "Minutes"

        // Remove any existing watcher to avoid multiple listeners on recycled views
        holder.textWatcher?.let { holder.timeLimit.removeTextChangedListener(it) }

        // Add a new TextWatcher to update the limits map as the user types
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                limits[app.packageName] = s.toString().toIntOrNull() ?: 0
            }
        }
        holder.timeLimit.addTextChangedListener(textWatcher)
        holder.textWatcher = textWatcher // Store watcher to remove it later
    }

    override fun getItemCount() = apps.size

    fun getTimeLimits(): Map<String, Int> {
        // The limits map is always up-to-date thanks to the TextWatcher.
        return limits
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appName: TextView = view.findViewById(R.id.appName)
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val timeLimit: EditText = view.findViewById(R.id.timeLimit)
        var textWatcher: TextWatcher? = null
    }
} 