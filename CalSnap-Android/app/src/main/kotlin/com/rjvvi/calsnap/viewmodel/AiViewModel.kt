package com.rjvvi.calsnap.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.rjvvi.calsnap.CalSnapApp
import com.rjvvi.calsnap.data.api.ChatMessage
import com.rjvvi.calsnap.data.prefs.UserPrefs
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AiViewModel(app: Application) : AndroidViewModel(app) {

    private val appObj = app as CalSnapApp
    private val gemini = appObj.gemini
    private val prefs = appObj.prefs

    val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    init {
        // Welcome message
        messages.value = listOf(
            ChatMessage("model", "Привет! 👋 Я AI-ассистент CalSnap. Помогу с вопросами о питании, калориях и диетах. Спрашивай!")
        )
    }

    fun sendMessage(text: String) = viewModelScope.launch {
        if (text.isBlank() || isLoading.value) return@launch

        val apiKey = prefs.prefs.first()[UserPrefs.GEMINI_API_KEY] ?: ""
        val model = prefs.prefs.first()[UserPrefs.GEMINI_MODEL] ?: "gemini-1.5-flash"

        if (apiKey.isBlank()) {
            error.value = "Введи API ключ Gemini в настройках ⚙️"
            return@launch
        }

        val userMsg = ChatMessage("user", text)
        val history = messages.value.toMutableList()
        messages.value = history + userMsg
        isLoading.value = true
        error.value = null

        // Build user context
        val prefs0 = prefs.prefs.first()
        val name = prefs0[UserPrefs.NAME] ?: ""
        val goal = prefs0[UserPrefs.GOAL] ?: "maintain"
        val cals = prefs0[UserPrefs.CALORIE_GOAL] ?: 2000
        val context = "Пользователь: ${name.ifBlank { "анонимно" }}, цель: $goal, норма: $cals ккал/день."

        gemini.chat(history.filter { it.role != "model" || history.indexOf(it) > 0 }, text, apiKey, model, context)
            .fold(
                onSuccess = { reply ->
                    messages.value = messages.value + ChatMessage("model", reply)
                },
                onFailure = {
                    error.value = it.message ?: "Ошибка соединения"
                }
            )
        isLoading.value = false
    }

    fun clearChat() {
        messages.value = listOf(
            ChatMessage("model", "Привет! 👋 Я AI-ассистент CalSnap. Помогу с вопросами о питании, калориях и диетах. Спрашивай!")
        )
        error.value = null
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AiViewModel(app) as T
        }
    }
}
