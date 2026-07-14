package com.example.studybuddyai.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studybuddyai.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TodoViewModel(
    private val repository: TaskRepository = InMemoryTaskRepository(),
    private val geminiService: GeminiService = GeminiService()
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _aiMessage = MutableStateFlow<String?>(null)
    val aiMessage: StateFlow<String?> = _aiMessage.asStateFlow()

    private val _isProcessingAi = MutableStateFlow(false)
    val isProcessingAi: StateFlow<Boolean> = _isProcessingAi.asStateFlow()

    val completedCount: StateFlow<Int> = tasks.map { list -> list.count { it.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCount: StateFlow<Int> = tasks.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addTaskFromNaturalLanguage(input: String) {
        viewModelScope.launch {
            _isProcessingAi.value = true
            try {
                val parsed = geminiService.parseNaturalLanguageTask(input)
                val task = Task(
                    title = parsed.title,
                    subject = parsed.subject,
                    description = parsed.description,
                    priority = parsed.priority,
                    category = parsed.category,
                    dueDate = parsed.dueDate
                )
                repository.addTask(task)

                // Auto-get a study tip
                val tip = geminiService.getStudyTip(task)
                _aiMessage.value = "✨ Task added! AI Tip: $tip"
            } catch (e: Exception) {
                repository.addTask(Task(title = input))
                _aiMessage.value = "Task added!"
            } finally {
                _isProcessingAi.value = false
            }
        }
    }

    fun addManualTask(title: String, subject: String, priority: Priority, category: TaskCategory) {
        val task = Task(
            title = title,
            subject = subject,
            priority = priority,
            category = category
        )
        repository.addTask(task)
    }

    fun toggleTask(taskId: String) {
        repository.toggleTask(taskId)
    }

    fun deleteTask(taskId: String) {
        repository.deleteTask(taskId)
    }

    fun toggleSubtask(taskId: String, subtaskId: String) {
        repository.toggleSubtask(taskId, subtaskId)
    }

    fun breakdownTask(task: Task) {
        viewModelScope.launch {
            _isProcessingAi.value = true
            try {
                val subtaskTitles = geminiService.breakdownTask(task)
                val subtasks = subtaskTitles.map { Subtask(title = it) }
                repository.addSubtasks(task.id, subtasks)
                _aiMessage.value = "🧠 AI broke down \"${task.title}\" into ${subtasks.size} subtasks!"
            } catch (e: Exception) {
                _aiMessage.value = "Couldn't break down task. Try again!"
            } finally {
                _isProcessingAi.value = false
            }
        }
    }

    fun getSmartPrioritization() {
        viewModelScope.launch {
            _isProcessingAi.value = true
            try {
                val recommendation = geminiService.smartPrioritize(tasks.value)
                _aiMessage.value = "🎯 $recommendation"
            } catch (e: Exception) {
                _aiMessage.value = "Focus on your most urgent tasks first!"
            } finally {
                _isProcessingAi.value = false
            }
        }
    }

    fun dismissAiMessage() {
        _aiMessage.value = null
    }
}
