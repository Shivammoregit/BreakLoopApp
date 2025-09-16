package com.example.testing

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SchedulingActivity : AppCompatActivity() {

    private lateinit var viewModel: TasksViewModel
    private lateinit var adapter: TasksAdapter

    private lateinit var inputName: TextInputEditText
    private lateinit var inputDesc: TextInputEditText
    private lateinit var nameLayout: TextInputLayout
    private lateinit var descLayout: TextInputLayout
    private lateinit var dueText: TextView
    private lateinit var clearDueButton: ImageButton
    private lateinit var fabAdd: FloatingActionButton

    private var selectedDueMillis: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheduling)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        inputName = findViewById(R.id.taskNameInput)
        inputDesc = findViewById(R.id.taskDescInput)
        nameLayout = findViewById(R.id.taskNameLayout)
        descLayout = findViewById(R.id.taskDescLayout)
        dueText = findViewById(R.id.dueText)
        clearDueButton = findViewById(R.id.clearDueButton)
        fabAdd = findViewById(R.id.fabAddTask)

        val database = TasksDatabase.getDatabase(this)
        val repository = TasksRepository(database.taskDao())
        val factory = TasksViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TasksViewModel::class.java]

        adapter = TasksAdapter(
            onCheckedChange = { task, isChecked -> viewModel.setCompleted(task.id, isChecked) },
            onEditClick = { task -> showEditDialog(task) },
            onDeleteClick = { task -> viewModel.deleteTask(task) }
        )

        findViewById<RecyclerView>(R.id.tasksRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@SchedulingActivity)
            adapter = this@SchedulingActivity.adapter
        }

        findViewById<View>(R.id.pickDueButton).setOnClickListener { pickDueDateTime() }
        clearDueButton.setOnClickListener { setDue(null) }

        fabAdd.setOnClickListener { addTask() }

        viewModel.tasks.observe(this) { tasks ->
            adapter.submitList(tasks)
        }

        lifecycleScope.launch {
            viewModel.error.collect { err ->
                err?.let {
                    Toast.makeText(this@SchedulingActivity, it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun addTask() {
        val name = inputName.text?.toString()?.trim() ?: ""
        val desc = inputDesc.text?.toString()?.trim()

        nameLayout.error = null
        if (TextUtils.isEmpty(name)) {
            nameLayout.error = getString(R.string.error_task_name_required)
            return
        }

        viewModel.addTask(name, if (desc.isNullOrEmpty()) null else desc, selectedDueMillis)
        inputName.setText("")
        inputDesc.setText("")
        setDue(null)
        Toast.makeText(this, R.string.msg_task_added, Toast.LENGTH_SHORT).show()
    }

    private fun showEditDialog(task: Task) {
        // Reuse top inputs for quick edit
        inputName.setText(task.name)
        inputDesc.setText(task.description ?: "")
        setDue(task.dueAtMillis)

        fabAdd.setOnClickListener {
            val newName = inputName.text?.toString()?.trim() ?: ""
            val newDesc = inputDesc.text?.toString()?.trim()
            if (newName.isEmpty()) {
                nameLayout.error = getString(R.string.error_task_name_required)
                return@setOnClickListener
            }
            viewModel.updateTask(
                task.copy(
                    name = newName,
                    description = if (newDesc.isNullOrEmpty()) null else newDesc,
                    dueAtMillis = selectedDueMillis
                )
            )
            inputName.setText("")
            inputDesc.setText("")
            setDue(null)
            fabAdd.setOnClickListener { addTask() } // Reset FAB to add mode
            Toast.makeText(this, R.string.msg_task_updated, Toast.LENGTH_SHORT).show()
        }
        Toast.makeText(this, R.string.hint_edit_mode, Toast.LENGTH_SHORT).show()
    }

    private fun pickDueDateTime() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val cal2 = Calendar.getInstance()
            cal2.set(Calendar.YEAR, y)
            cal2.set(Calendar.MONTH, m)
            cal2.set(Calendar.DAY_OF_MONTH, d)
            TimePickerDialog(this, { _, h, min ->
                cal2.set(Calendar.HOUR_OF_DAY, h)
                cal2.set(Calendar.MINUTE, min)
                cal2.set(Calendar.SECOND, 0)
                setDue(cal2.timeInMillis)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setDue(millis: Long?) {
        selectedDueMillis = millis
        if (millis == null) {
            dueText.text = getString(R.string.label_no_due)
            clearDueButton.visibility = View.GONE
        } else {
            val df = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
            dueText.text = df.format(Date(millis))
            clearDueButton.visibility = View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}