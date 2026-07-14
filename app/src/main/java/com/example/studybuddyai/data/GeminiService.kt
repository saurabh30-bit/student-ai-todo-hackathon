package com.example.studybuddyai.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.example.studybuddyai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService {
    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 1024
            }
        )
    }

    suspend fun parseNaturalLanguageTask(input: String): ParsedTask = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are a student task assistant. Parse the following natural language input into a structured task.
                Return ONLY a response in this exact format (no extra text):
                TITLE: <concise task title>
                SUBJECT: <academic subject like Math, Physics, English, Computer Science, etc. Use "General" if unclear>
                PRIORITY: <URGENT or HIGH or MEDIUM or LOW based on urgency>
                CATEGORY: <HOMEWORK or EXAM or PROJECT or READING or LAB or OTHER>
                DUE: <due date if mentioned, otherwise "">
                DESCRIPTION: <brief 1-line description>

                Student input: "$input"
            """.trimIndent()

            val response = model.generateContent(prompt)
            val text = response.text ?: ""

            val title = extractField(text, "TITLE") ?: input
            val subject = extractField(text, "SUBJECT") ?: ""
            val priority = try {
                Priority.valueOf(extractField(text, "PRIORITY") ?: "MEDIUM")
            } catch (e: Exception) { Priority.MEDIUM }
            val category = try {
                TaskCategory.valueOf(extractField(text, "CATEGORY") ?: "OTHER")
            } catch (e: Exception) { TaskCategory.OTHER }
            val due = extractField(text, "DUE") ?: ""
            val description = extractField(text, "DESCRIPTION") ?: ""

            ParsedTask(title, subject, priority, category, due, description)
        } catch (e: Exception) {
            ParsedTask(title = input, subject = "", priority = Priority.MEDIUM, category = TaskCategory.OTHER, dueDate = "", description = "")
        }
    }

    suspend fun breakdownTask(task: Task): List<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are a student study assistant. Break down this academic task into 3-6 actionable subtasks.
                Return ONLY the subtasks as a numbered list (1. 2. 3. etc.), nothing else.

                Task: "${task.title}"
                Subject: "${task.subject}"
                Category: "${task.category.label}"
            """.trimIndent()

            val response = model.generateContent(prompt)
            val text = response.text ?: ""

            text.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.matches(Regex("^\\d+\\..*")) }
                .map { it.replaceFirst(Regex("^\\d+\\.\\s*"), "") }
                .take(6)
        } catch (e: Exception) {
            listOf("Start working on ${task.title}", "Review and finalize")
        }
    }

    suspend fun getStudyTip(task: Task): String = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Give ONE concise, practical study tip (max 2 sentences) for a student working on:
                Task: "${task.title}"
                Subject: "${task.subject}"
                Category: "${task.category.label}"
                
                Be encouraging and specific. Return ONLY the tip, nothing else.
            """.trimIndent()

            val response = model.generateContent(prompt)
            response.text?.trim() ?: "Stay focused and take short breaks every 25 minutes! 💪"
        } catch (e: Exception) {
            "Stay focused and take short breaks every 25 minutes! 💪"
        }
    }

    suspend fun smartPrioritize(tasks: List<Task>): String = withContext(Dispatchers.IO) {
        try {
            val taskList = tasks.filter { !it.isCompleted }.joinToString("\n") { 
                "- ${it.title} (${it.subject}, ${it.priority.label}, due: ${it.dueDate.ifEmpty { "no date" }})" 
            }
            val prompt = """
                You are a student productivity coach. Given these tasks, suggest the optimal order to tackle them.
                Give a brief 2-3 sentence recommendation on what to focus on first and why.
                
                Tasks:
                $taskList
                
                Return ONLY your recommendation, be encouraging and concise.
            """.trimIndent()

            val response = model.generateContent(prompt)
            response.text?.trim() ?: "Focus on your most urgent tasks first!"
        } catch (e: Exception) {
            "Focus on your most urgent tasks first! You've got this! 🎯"
        }
    }

    private fun extractField(text: String, field: String): String? {
        val regex = Regex("$field:\\s*(.*)", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() && it != "\"\"" }
    }
}

data class ParsedTask(
    val title: String,
    val subject: String,
    val priority: Priority,
    val category: TaskCategory,
    val dueDate: String,
    val description: String
)
