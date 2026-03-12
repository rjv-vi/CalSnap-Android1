package com.rjvvi.calsnap.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // ── FOOD ENTRIES ──
    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY timeAdded ASC")
    fun getFoodByDate(date: String): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries WHERE date >= :fromDate ORDER BY date ASC, timeAdded ASC")
    fun getFoodFrom(fromDate: String): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries ORDER BY date DESC, timeAdded DESC LIMIT 200")
    fun getAllFood(): Flow<List<FoodEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(entry: FoodEntry): Long

    @Update
    suspend fun updateFood(entry: FoodEntry)

    @Delete
    suspend fun deleteFood(entry: FoodEntry)

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteFoodById(id: Long)

    // ── WEIGHT ENTRIES ──
    @Query("SELECT * FROM weight_entries ORDER BY date DESC")
    fun getAllWeights(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries WHERE date >= :fromDate ORDER BY date ASC")
    fun getWeightsFrom(fromDate: String): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWeight(): WeightEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(entry: WeightEntry)

    @Delete
    suspend fun deleteWeight(entry: WeightEntry)

    // ── WATER ENTRIES ──
    @Query("SELECT * FROM water_entries WHERE date = :date ORDER BY timestamp ASC")
    fun getWaterByDate(date: String): Flow<List<WaterEntry>>

    @Query("SELECT SUM(amountMl) FROM water_entries WHERE date = :date")
    fun getTotalWaterByDate(date: String): Flow<Int?>

    @Insert
    suspend fun insertWater(entry: WaterEntry)

    @Delete
    suspend fun deleteWater(entry: WaterEntry)

    @Query("DELETE FROM water_entries WHERE date = :date AND id = (SELECT MAX(id) FROM water_entries WHERE date = :date)")
    suspend fun deleteLastWater(date: String)

    // ── FAVOURITES ──
    @Query("SELECT * FROM favourites ORDER BY addedAt DESC")
    fun getAllFavourites(): Flow<List<FavouriteEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavourite(entry: FavouriteEntry)

    @Delete
    suspend fun deleteFavourite(entry: FavouriteEntry)

    @Query("DELETE FROM favourites WHERE id = :id")
    suspend fun deleteFavouriteById(id: Long)

    // ── STATS ──
    @Query("SELECT DISTINCT date FROM food_entries ORDER BY date DESC")
    suspend fun getAllLoggedDates(): List<String>

    @Query("DELETE FROM food_entries")
    suspend fun deleteAllFood()

    @Query("DELETE FROM weight_entries")
    suspend fun deleteAllWeights()

    @Query("DELETE FROM water_entries")
    suspend fun deleteAllWater()

    @Query("DELETE FROM favourites")
    suspend fun deleteAllFavourites()
}
