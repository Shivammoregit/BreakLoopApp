package com.example.testing

import androidx.lifecycle.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TasksViewModel(private val repository: TasksRepository) : ViewModel() {

    val tasks: LiveData<List<Task>> = repository.allTasks.asLiveData()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun addTask(name: String, description: String?, dueAtMillis: Long?) {
        viewModelScope.launch {
            val task = Task(name = name, description = description, dueAtMillis = dueAtMillis)
            repository.insert(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.update(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }

    fun setCompleted(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.setCompleted(taskId, isCompleted)
        }
    }

    fun clearError() {
        _error.value = null
    }
}

class TasksViewModelFactory(private val repository: TasksRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TasksViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}