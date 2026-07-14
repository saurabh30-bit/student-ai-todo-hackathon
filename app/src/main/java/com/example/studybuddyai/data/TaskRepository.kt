package com.example.studybuddyai.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

interface TaskRepository {
    val tasks: Flow<List<Task>>
    fun addTask(task: Task)
    fun updateTask(task: Task)
    fun deleteTask(taskId: String)
    fun toggleTask(taskId: String)
    fun toggleSubtask(taskId: String, subtaskId: String)
    fun addSubtasks(taskId: String, subtasks: List<Subtask>)
}

class InMemoryTaskRepository : TaskRepository {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    override val tasks: Flow<List<Task>> = _tasks

    override fun addTask(task: Task) {
        _tasks.value = _tasks.value + task
    }

    override fun updateTask(task: Task) {
        _tasks.value = _tasks.value.map { if (it.id == task.id) task else it }
    }

    override fun deleteTask(taskId: String) {
        _tasks.value = _tasks.value.filter { it.id != taskId }
    }

    override fun toggleTask(taskId: String) {
        _tasks.value = _tasks.value.map {
            if (it.id == taskId) it.copy(isCompleted = !it.isCompleted) else it
        }
    }

    override fun toggleSubtask(taskId: String, subtaskId: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(subtasks = task.subtasks.map { sub ->
                    if (sub.id == subtaskId) sub.copy(isCompleted = !sub.isCompleted) else sub
                })
            } else task
        }
    }

    override fun addSubtasks(taskId: String, subtasks: List<Subtask>) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) task.copy(subtasks = task.subtasks + subtasks) else task
        }
    }
}
