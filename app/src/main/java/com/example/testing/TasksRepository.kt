package com.example.testing

import kotlinx.coroutines.flow.Flow

class TasksRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun insert(task: Task) {
        taskDao.insert(task)
    }

    suspend fun update(task: Task) {
        taskDao.update(task)
    }

    suspend fun delete(task: Task) {
        taskDao.delete(task)
    }

    suspend fun setCompleted(taskId: Long, isCompleted: Boolean) = taskDao.setCompleted(taskId, isCompleted)
}