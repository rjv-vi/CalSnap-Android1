package com.rjvvi.calsnap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.google.gson.Gson
import com.rjvvi.calsnap.CalSnapApp
import com.rjvvi.calsnap.data.db.*
import com.rjvvi.calsnap.data.prefs.UserPrefs
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val appObj = app as CalSnapApp
    private val db = appObj.db.dao()
    val prefs = appObj.prefs
    val gemini = appObj.gemini

    val theme: StateFlow<String> = prefs.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val soundsEnabled: StateFlow<Boolean> = prefs.soundsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hapticEnabled: StateFlow<Boolean> = prefs.hapticEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notifEnabled: StateFlow<Boolean> = prefs.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val apiKey: StateFlow<String> = prefs.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val geminiModel: StateFlow<String> = prefs.geminiModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemini-1.5-flash")

    val availableModels = MutableStateFlow<List<String>>(emptyList())
    val modelsLoading = MutableStateFlow(false)

    val calorieGoal: StateFlow<Int> = prefs.calorieGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val waterGoal: StateFlow<Int> = prefs.waterGoalMl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val name: StateFlow<String> = prefs.name
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val weightKg: StateFlow<Float> = prefs.weightKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70f)

    val heightCm: StateFlow<Float> = prefs.heightCm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 170f)

    val streak: StateFlow<Int> = prefs.streak
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setTheme(t: String) = viewModelScope.launch { prefs.set(UserPrefs.THEME, t) }
    fun setSounds(v: Boolean) = viewModelScope.launch { prefs.set(UserPrefs.SOUNDS_ENABLED, v) }
    fun setHaptic(v: Boolean) = viewModelScope.launch { prefs.set(UserPrefs.HAPTIC_ENABLED, v) }
    fun setNotif(v: Boolean) = viewModelScope.launch { prefs.set(UserPrefs.NOTIFICATIONS_ENABLED, v) }
    fun setApiKey(k: String) = viewModelScope.launch { prefs.set(UserPrefs.GEMINI_API_KEY, k) }
    fun setModel(m: String) = viewModelScope.launch { prefs.set(UserPrefs.GEMINI_MODEL, m) }
    fun setCalorieGoal(v: Int) = viewModelScope.launch { prefs.set(UserPrefs.CALORIE_GOAL, v) }
    fun setWaterGoal(v: Int) = viewModelScope.launch { prefs.set(UserPrefs.WATER_GOAL_ML, v) }
    fun setName(v: String) = viewModelScope.launch { prefs.set(UserPrefs.NAME, v) }

    fun loadModels() = viewModelScope.launch {
        val key = prefs.prefs.first()[UserPrefs.GEMINI_API_KEY] ?: ""
        if (key.isBlank()) return@launch
        modelsLoading.value = true
        gemini.listModels(key).fold(
            onSuccess = { availableModels.value = it },
            onFailure = { }
        )
        modelsLoading.value = false
    }

    fun resetAll() = viewModelScope.launch {
        db.deleteAllFood()
        db.deleteAllWeights()
        db.deleteAllWater()
        db.deleteAllFavourites()
        prefs.clearAll()
    }

    data class ExportData(
        val food: List<FoodEntry>,
        val weights: List<WeightEntry>,
        val exportDate: String
    )

    suspend fun exportJson(context: Context): File? {
        return try {
            val food = db.getAllFood().first()
            val weights = db.getAllWeights().first()
            val data = ExportData(
                food = food, weights = weights,
                exportDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            )
            val json = Gson().toJson(data)
            val file = File(context.cacheDir, "calsnap_export_${System.currentTimeMillis()}.json")
            file.writeText(json)
            file
        } catch (e: Exception) {
            null
        }
    }

    fun importJson(json: String) = viewModelScope.launch {
        try {
            val data = Gson().fromJson(json, ExportData::class.java)
            data.food.forEach { db.insertFood(it.copy(id = 0)) }
        } catch (e: Exception) { /* ignore */ }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(app) as T
        }
    }
}
