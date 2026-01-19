package com.healthio.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert
    suspend fun insertMeal(meal: MealLog)

    @Query("SELECT * FROM meal_logs ORDER BY timestamp DESC")
    fun getAllMeals(): Flow<List<MealLog>>

    // Aggregations
    @Query("SELECT SUM(calories) FROM meal_logs WHERE timestamp >= :start AND timestamp < :end")
    fun getCaloriesBetween(start: Long, end: Long): Flow<Int?>

    @Query("SELECT SUM(protein) FROM meal_logs WHERE timestamp >= :start AND timestamp < :end")
    fun getProteinBetween(start: Long, end: Long): Flow<Int?>

    @Query("SELECT SUM(carbs) FROM meal_logs WHERE timestamp >= :start AND timestamp < :end")
    fun getCarbsBetween(start: Long, end: Long): Flow<Int?>

    @Query("SELECT SUM(fat) FROM meal_logs WHERE timestamp >= :start AND timestamp < :end")
    fun getFatBetween(start: Long, end: Long): Flow<Int?>

    @Query("SELECT * FROM meal_logs WHERE isSynced = 0")
    suspend fun getUnsyncedMeals(): List<MealLog>

    @Query("UPDATE meal_logs SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM meal_logs WHERE timestamp >= :start AND timestamp < :end")
    suspend fun getCountBetween(start: Long, end: Long): Int
}
