package com.rjvvi.calsnap.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPrefs(private val context: Context) {

    companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val NAME = stringPreferencesKey("name")
        val GENDER = stringPreferencesKey("gender")           // male / female
        val DATE_OF_BIRTH = stringPreferencesKey("dob")       // "2000-01-15"
        val HEIGHT_CM = floatPreferencesKey("height_cm")
        val WEIGHT_KG = floatPreferencesKey("weight_kg")
        val GOAL = stringPreferencesKey("goal")               // lose / maintain / gain
        val ACTIVITY = stringPreferencesKey("activity")       // sedentary / light / moderate / active / very_active
        val CALORIE_GOAL = intPreferencesKey("calorie_goal")
        val PROTEIN_GOAL = intPreferencesKey("protein_goal")
        val CARBS_GOAL = intPreferencesKey("carbs_goal")
        val FAT_GOAL = intPreferencesKey("fat_goal")
        val WATER_GOAL_ML = intPreferencesKey("water_goal_ml")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val THEME = stringPreferencesKey("theme")             // light / dark / system
        val SOUNDS_ENABLED = booleanPreferencesKey("sounds_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIF_MORNING = stringPreferencesKey("notif_morning")   // "08:00"
        val NOTIF_LUNCH = stringPreferencesKey("notif_lunch")       // "13:00"
        val NOTIF_EVENING = stringPreferencesKey("notif_evening")   // "19:00"
        val STREAK = intPreferencesKey("streak")
        val LAST_LOG_DATE = stringPreferencesKey("last_log_date")
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")       // metric / imperial
    }

    val prefs: Flow<Preferences> = context.dataStore.data

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_DONE] ?: false }
    val geminiApiKey: Flow<String> = context.dataStore.data.map { it[GEMINI_API_KEY] ?: "" }
    val geminiModel: Flow<String> = context.dataStore.data.map { it[GEMINI_MODEL] ?: "gemini-1.5-flash" }
    val theme: Flow<String> = context.dataStore.data.map { it[THEME] ?: "system" }
    val calorieGoal: Flow<Int> = context.dataStore.data.map { it[CALORIE_GOAL] ?: 2000 }
    val waterGoalMl: Flow<Int> = context.dataStore.data.map { it[WATER_GOAL_ML] ?: 2000 }
    val soundsEnabled: Flow<Boolean> = context.dataStore.data.map { it[SOUNDS_ENABLED] ?: true }
    val hapticEnabled: Flow<Boolean> = context.dataStore.data.map { it[HAPTIC_ENABLED] ?: true }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: false }
    val streak: Flow<Int> = context.dataStore.data.map { it[STREAK] ?: 0 }
    val name: Flow<String> = context.dataStore.data.map { it[NAME] ?: "" }
    val gender: Flow<String> = context.dataStore.data.map { it[GENDER] ?: "male" }
    val weightKg: Flow<Float> = context.dataStore.data.map { it[WEIGHT_KG] ?: 70f }
    val heightCm: Flow<Float> = context.dataStore.data.map { it[HEIGHT_CM] ?: 170f }
    val goal: Flow<String> = context.dataStore.data.map { it[GOAL] ?: "maintain" }
    val activity: Flow<String> = context.dataStore.data.map { it[ACTIVITY] ?: "moderate" }

    suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }
    suspend fun set(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
    }
    suspend fun set(key: Preferences.Key<Int>, value: Int) {
        context.dataStore.edit { it[key] = value }
    }
    suspend fun set(key: Preferences.Key<Float>, value: Float) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun saveProfile(
        name: String, gender: String, dob: String,
        height: Float, weight: Float,
        goal: String, activity: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[NAME] = name
            prefs[GENDER] = gender
            prefs[DATE_OF_BIRTH] = dob
            prefs[HEIGHT_CM] = height
            prefs[WEIGHT_KG] = weight
            prefs[GOAL] = goal
            prefs[ACTIVITY] = activity
            prefs[ONBOARDING_DONE] = true
            val cals = calcCalories(gender, dob, height, weight, activity, goal)
            prefs[CALORIE_GOAL] = cals
            prefs[PROTEIN_GOAL] = (weight * 1.6).toInt()
            prefs[CARBS_GOAL] = ((cals * 0.45) / 4).toInt()
            prefs[FAT_GOAL] = ((cals * 0.30) / 9).toInt()
            prefs[WATER_GOAL_ML] = 2000
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}

fun calcCalories(
    gender: String, dob: String,
    height: Float, weight: Float,
    activity: String, goal: String
): Int {
    val age = try {
        val parts = dob.split("-")
        val year = parts[0].toInt()
        val now = java.util.Calendar.getInstance()
        now.get(java.util.Calendar.YEAR) - year
    } catch (e: Exception) { 25 }

    // Mifflin–St Jeor
    val bmr = if (gender == "male") {
        10 * weight + 6.25 * height - 5 * age + 5
    } else {
        10 * weight + 6.25 * height - 5 * age - 161
    }

    val pal = when (activity) {
        "sedentary" -> 1.2
        "light" -> 1.375
        "moderate" -> 1.55
        "active" -> 1.725
        "very_active" -> 1.9
        else -> 1.55
    }

    val tdee = bmr * pal
    val adjusted = when (goal) {
        "lose" -> tdee - 500
        "gain" -> tdee + 300
        else -> tdee
    }
    return adjusted.toInt().coerceAtLeast(1200)
}
