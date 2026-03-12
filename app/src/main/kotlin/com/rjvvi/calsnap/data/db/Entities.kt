package com.rjvvi.calsnap.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,           // "2024-01-15"
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val portion: String = "100 г",
    val mealType: String = "snack", // breakfast, lunch, dinner, snack
    val timeAdded: Long = System.currentTimeMillis(),
    val imageUri: String? = null
)

@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val weight: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favourites")
data class FavouriteEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val portion: String = "100 г",
    val addedAt: Long = System.currentTimeMillis()
)
