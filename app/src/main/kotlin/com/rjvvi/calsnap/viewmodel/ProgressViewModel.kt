package com.rjvvi.calsnap.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.rjvvi.calsnap.CalSnapApp
import com.rjvvi.calsnap.data.db.FoodEntry
import com.rjvvi.calsnap.data.db.WeightEntry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class DaySummary(
    val date: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val label: String // "Пн", "Вт", etc.
)

class ProgressViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as CalSnapApp).db.dao()
    val prefs = app.prefs

    val allWeights: StateFlow<List<WeightEntry>> = db.getAllWeights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calorieGoal: StateFlow<Int> = prefs.calorieGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    // Last 7 days
    val weeklyData: StateFlow<List<DaySummary>> = db.getFoodFrom(sevenDaysAgo())
        .map { entries -> buildWeeklySummary(entries) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Last 30 days food
    val monthlyData: StateFlow<List<DaySummary>> = db.getFoodFrom(thirtyDaysAgo())
        .map { entries -> buildMonthlySummary(entries) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logWeight(kg: Float) = viewModelScope.launch {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        db.insertWeight(WeightEntry(date = today, weight = kg))
        prefs.set(com.rjvvi.calsnap.data.prefs.UserPrefs.WEIGHT_KG, kg)
    }

    fun deleteWeight(entry: WeightEntry) = viewModelScope.launch {
        db.deleteWeight(entry)
    }

    private fun sevenDaysAgo(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    private fun thirtyDaysAgo(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -29)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    private fun buildWeeklySummary(entries: List<FoodEntry>): List<DaySummary> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNames = mapOf(
            Calendar.MONDAY to "Пн", Calendar.TUESDAY to "Вт",
            Calendar.WEDNESDAY to "Ср", Calendar.THURSDAY to "Чт",
            Calendar.FRIDAY to "Пт", Calendar.SATURDAY to "Сб",
            Calendar.SUNDAY to "Вс"
        )
        val grouped = entries.groupBy { it.date }
        val result = mutableListOf<DaySummary>()
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(cal.time)
            val dayEntries = grouped[dateStr] ?: emptyList()
            val dayName = if (i == 0) "Сег" else dayNames[cal.get(Calendar.DAY_OF_WEEK)] ?: "?"
            result.add(
                DaySummary(
                    date = dateStr,
                    calories = dayEntries.sumOf { it.calories },
                    protein = dayEntries.sumOf { it.protein.toDouble() }.toFloat(),
                    carbs = dayEntries.sumOf { it.carbs.toDouble() }.toFloat(),
                    fat = dayEntries.sumOf { it.fat.toDouble() }.toFloat(),
                    label = dayName
                )
            )
        }
        return result
    }

    private fun buildMonthlySummary(entries: List<FoodEntry>): List<DaySummary> {
        val grouped = entries.groupBy { it.date }
        return grouped.entries
            .sortedBy { it.key }
            .map { (date, dayEntries) ->
                DaySummary(
                    date = date,
                    calories = dayEntries.sumOf { it.calories },
                    protein = dayEntries.sumOf { it.protein.toDouble() }.toFloat(),
                    carbs = dayEntries.sumOf { it.carbs.toDouble() }.toFloat(),
                    fat = dayEntries.sumOf { it.fat.toDouble() }.toFloat(),
                    label = date.takeLast(5)
                )
            }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ProgressViewModel(app) as T
        }
    }
}
