package com.example.testing

import android.app.AlertDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class NotesActivity : AppCompatActivity() {
    
    private lateinit var viewModel: NotesViewModel
    private lateinit var adapter: NotesAdapter
    private lateinit var titleInput: TextInputEditText
    private lateinit var bodyInput: TextInputEditText
    private lateinit var titleLayout: TextInputLayout
    private lateinit var bodyLayout: TextInputLayout
    private lateinit var addNoteButton: MaterialButton
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var moodHappy: MaterialButton
    private lateinit var moodStressed: MaterialButton
    private lateinit var moodCalm: MaterialButton
    
    private var selectedMood: String? = null
    private var editingNote: Note? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)
        
        setupToolbar()
        setupViews()
        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }
    
    private fun setupViews() {
        titleInput = findViewById(R.id.titleInput)
        bodyInput = findViewById(R.id.bodyInput)
        titleLayout = findViewById(R.id.titleInputLayout)
        bodyLayout = findViewById(R.id.bodyInputLayout)
        addNoteButton = findViewById(R.id.addNoteButton)
        fabAddNote = findViewById(R.id.fabAddNote)
        moodHappy = findViewById(R.id.moodHappy)
        moodStressed = findViewById(R.id.moodStressed)
        moodCalm = findViewById(R.id.moodCalm)
    }
    
    private fun setupViewModel() {
        val database = NotesDatabase.getDatabase(this)
        val repository = NotesRepository(database.noteDao())
        val factory = NotesViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[NotesViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        adapter = NotesAdapter(
            onEditClick = { note -> editNote(note) },
            onDeleteClick = { note -> deleteNote(note) }
        )
        
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.notesRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@NotesActivity)
            adapter = this@NotesActivity.adapter
        }
    }
    
    private fun setupClickListeners() {
        addNoteButton.setOnClickListener { addOrUpdateNote() }
        fabAddNote.setOnClickListener { scrollToAddNote() }
        
        // Mood selection
        moodHappy.setOnClickListener { selectMood("Happy", moodHappy) }
        moodStressed.setOnClickListener { selectMood("Stressed", moodStressed) }
        moodCalm.setOnClickListener { selectMood("Calm", moodCalm) }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.notes.collect { notes ->
                adapter.submitList(notes)
            }
        }
        
        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(this@NotesActivity, it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }
    
    private fun selectMood(mood: String, button: MaterialButton) {
        // Reset all mood buttons
        resetMoodButtons()
        
        // Select current mood
        selectedMood = mood
        button.isSelected = true
        button.setBackgroundColor(getColor(R.color.colorPrimary))
        button.setTextColor(getColor(R.color.white))
    }
    
    private fun resetMoodButtons() {
        val buttons = listOf(moodHappy, moodStressed, moodCalm)
        buttons.forEach { button ->
            button.isSelected = false
            button.setBackgroundColor(getColor(android.R.color.transparent))
            button.setTextColor(getColor(R.color.textPrimary))
        }
    }
    
    private fun addOrUpdateNote() {
        val title = titleInput.text.toString().trim()
        val body = bodyInput.text.toString().trim()
        
        // Clear previous errors
        titleLayout.error = null
        bodyLayout.error = null
        
        var hasError = false
        
        if (TextUtils.isEmpty(title)) {
            titleLayout.error = "Title is required"
            hasError = true
        }
        
        if (TextUtils.isEmpty(body)) {
            bodyLayout.error = "Note content is required"
            hasError = true
        }
        
        if (hasError) return
        
        if (editingNote != null) {
            // Update existing note
            val updatedNote = editingNote!!.copy(
                title = title,
                body = body,
                mood = selectedMood,
                updatedAt = System.currentTimeMillis()
            )
            viewModel.updateNote(updatedNote)
            Toast.makeText(this, "Note updated successfully", Toast.LENGTH_SHORT).show()
        } else {
            // Add new note
            viewModel.addNote(title, body, selectedMood)
            Toast.makeText(this, "Note added successfully", Toast.LENGTH_SHORT).show()
        }
        
        clearForm()
    }
    
    private fun editNote(note: Note) {
        editingNote = note
        titleInput.setText(note.title)
        bodyInput.setText(note.body)
        
        // Set mood if available
        when (note.mood) {
            "Happy" -> selectMood("Happy", moodHappy)
            "Stressed" -> selectMood("Stressed", moodStressed)
            "Calm" -> selectMood("Calm", moodCalm)
            else -> resetMoodButtons()
        }
        
        addNoteButton.text = "Update Note"
        
        // Scroll to top to show the form
        scrollToAddNote()
    }
    
    private fun deleteNote(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteNote(note)
                Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun clearForm() {
        titleInput.setText("")
        bodyInput.setText("")
        resetMoodButtons()
        selectedMood = null
        editingNote = null
        addNoteButton.text = "Add Note"
        
        // Clear errors
        titleLayout.error = null
        bodyLayout.error = null
    }
    
    private fun scrollToAddNote() {
        findViewById<androidx.core.widget.NestedScrollView>(android.R.id.content)
            .scrollTo(0, 0)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
