package com.rjvvi.calsnap.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.*
import com.rjvvi.calsnap.CalSnapApp
import com.rjvvi.calsnap.data.api.FoodAnalysisResult
import com.rjvvi.calsnap.data.db.FavouriteEntry
import com.rjvvi.calsnap.data.db.FoodEntry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class AddFoodState {
    object Idle : AddFoodState()
    object Loading : AddFoodState()
    data class Success(val result: FoodAnalysisResult) : AddFoodState()
    data class Error(val message: String) : AddFoodState()
}

class AddFoodViewModel(app: Application) : AndroidViewModel(app) {

    private val appObj = app as CalSnapApp
    private val db = appObj.db.dao()
    private val gemini = appObj.gemini
    private val prefs = appObj.prefs

    val state = MutableStateFlow<AddFoodState>(AddFoodState.Idle)
    val favourites: StateFlow<List<FavouriteEntry>> = db.getAllFavourites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentResult: FoodAnalysisResult? = null

    fun analyzePhoto(bitmap: Bitmap, hint: String = "") = viewModelScope.launch {
        state.value = AddFoodState.Loading
        val apiKey = prefs.prefs.first()[com.rjvvi.calsnap.data.prefs.UserPrefs.GEMINI_API_KEY] ?: ""
        val model = prefs.prefs.first()[com.rjvvi.calsnap.data.prefs.UserPrefs.GEMINI_MODEL] ?: "gemini-1.5-flash"
        if (apiKey.isBlank()) {
            state.value = AddFoodState.Error("Введи API ключ Gemini в настройках")
            return@launch
        }
        gemini.analyzePhoto(bitmap, apiKey, model, hint).fold(
            onSuccess = { result ->
                currentResult = result
                state.value = AddFoodState.Success(result)
            },
            onFailure = { state.value = AddFoodState.Error(it.message ?: "Ошибка анализа") }
        )
    }

    fun analyzeText(text: String) = viewModelScope.launch {
        state.value = AddFoodState.Loading
        val apiKey = prefs.prefs.first()[com.rjvvi.calsnap.data.prefs.UserPrefs.GEMINI_API_KEY] ?: ""
        val model = prefs.prefs.first()[com.rjvvi.calsnap.data.prefs.UserPrefs.GEMINI_MODEL] ?: "gemini-1.5-flash"
        if (apiKey.isBlank()) {
            state.value = AddFoodState.Error("Введи API ключ Gemini в настройках")
            return@launch
        }
        gemini.analyzeText(text, apiKey, model).fold(
            onSuccess = { result ->
                currentResult = result
                state.value = AddFoodState.Success(result)
            },
            onFailure = { state.value = AddFoodState.Error(it.message ?: "Ошибка анализа") }
        )
    }

    fun analyzeBarcode(barcode: String) = viewModelScope.launch {
        state.value = AddFoodState.Loading
        val apiKey = prefs.prefs.first()[com.rjvvi.calsnap.data.prefs.UserPrefs.GEMINI_API_KEY] ?: ""
        val model = prefs.prefs.first()[com.rjvvi.calsnap.data.prefs.UserPrefs.GEMINI_MODEL] ?: "gemini-1.5-flash"
        if (apiKey.isBlank()) {
            state.value = AddFoodState.Error("Введи API ключ Gemini в настройках")
            return@launch
        }
        gemini.lookupBarcode(barcode, apiKey, model).fold(
            onSuccess = { result ->
                currentResult = result
                state.value = AddFoodState.Success(result)
            },
            onFailure = { state.value = AddFoodState.Error(it.message ?: "Ошибка поиска штрихкода") }
        )
    }

    fun saveToFavourites(result: FoodAnalysisResult) = viewModelScope.launch {
        db.insertFavourite(
            FavouriteEntry(
                name = result.name, calories = result.calories,
                protein = result.protein, carbs = result.carbs,
                fat = result.fat, portion = result.portion
            )
        )
    }

    fun deleteFavourite(id: Long) = viewModelScope.launch {
        db.deleteFavouriteById(id)
    }

    fun reset() {
        state.value = AddFoodState.Idle
        currentResult = null
    }

    fun addToJournal(
        result: FoodAnalysisResult,
        mealType: String,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): FoodEntry {
        return FoodEntry(
            date = date,
            name = result.name,
            calories = result.calories,
            protein = result.protein,
            carbs = result.carbs,
            fat = result.fat,
            portion = result.portion,
            mealType = mealType
        )
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AddFoodViewModel(app) as T
        }
    }
}
