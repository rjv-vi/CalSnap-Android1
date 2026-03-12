package com.rjvvi.calsnap.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.rjvvi.calsnap.CalSnapApp
import com.rjvvi.calsnap.data.db.*
import com.rjvvi.calsnap.data.prefs.UserPrefs
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as CalSnapApp).db.dao()
    val prefs = app.prefs

    val todayDate: String get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val todayFood: StateFlow<List<FoodEntry>> = db.getFoodByDate(todayDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayWater: StateFlow<Int> = db.getTotalWaterByDate(todayDate)
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val calorieGoal: StateFlow<Int> = prefs.calorieGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val waterGoal: StateFlow<Int> = prefs.waterGoalMl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val streak: StateFlow<Int> = prefs.streak
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val userName: StateFlow<String> = prefs.name
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Computed totals
    val todayCalories: StateFlow<Int> = todayFood
        .map { list -> list.sumOf { it.calories } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayProtein: StateFlow<Float> = todayFood
        .map { list -> list.sumOf { it.protein.toDouble() }.toFloat() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val todayCarbs: StateFlow<Float> = todayFood
        .map { list -> list.sumOf { it.carbs.toDouble() }.toFloat() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val todayFat: StateFlow<Float> = todayFood
        .map { list -> list.sumOf { it.fat.toDouble() }.toFloat() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    fun addFood(entry: FoodEntry) = viewModelScope.launch {
        db.insertFood(entry)
        updateStreak()
    }

    fun deleteFood(entry: FoodEntry) = viewModelScope.launch {
        db.deleteFood(entry)
    }

    fun updateFood(entry: FoodEntry) = viewModelScope.launch {
        db.updateFood(entry)
    }

    fun addWater(amount: Int) = viewModelScope.launch {
        db.insertWater(WaterEntry(date = todayDate, amountMl = amount))
    }

    fun undoWater() = viewModelScope.launch {
        db.deleteLastWater(todayDate)
    }

    private suspend fun updateStreak() {
        val today = todayDate
        val lastLog = prefs.prefs.first()[UserPrefs.LAST_LOG_DATE]
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayCal = sdf.parse(today)
        val lastCal = if (lastLog != null) sdf.parse(lastLog) else null
        val currentStreak = prefs.prefs.first()[UserPrefs.STREAK] ?: 0

        val newStreak = when {
            lastLog == null || lastCal == null -> 1
            lastLog == today -> currentStreak
            else -> {
                val diff = ((todayCal!!.time - lastCal.time) / (1000 * 60 * 60 * 24)).toInt()
                if (diff == 1) currentStreak + 1 else 1
            }
        }

        prefs.set(UserPrefs.STREAK, newStreak)
        prefs.set(UserPrefs.LAST_LOG_DATE, today)
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(app) as T
        }
    }
}
