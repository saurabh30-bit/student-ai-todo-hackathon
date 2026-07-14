package com.example.studybuddyai.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subject: String = "",
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val category: TaskCategory = TaskCategory.HOMEWORK,
    val isCompleted: Boolean = false,
    val dueDate: String = "",
    val subtasks: List<Subtask> = emptyList(),
    val aiSuggestion: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)

enum class Priority(val label: String, val color: Long) {
    URGENT("Urgent", 0xFFFF5252),
    HIGH("High", 0xFFFF9800),
    MEDIUM("Medium", 0xFF4CAF50),
    LOW("Low", 0xFF2196F3)
}

enum class TaskCategory(val label: String, val icon: String) {
    HOMEWORK("Homework", "📝"),
    EXAM("Exam Prep", "📖"),
    PROJECT("Project", "🚀"),
    READING("Reading", "📚"),
    LAB("Lab Work", "🔬"),
    OTHER("Other", "📌")
}
