package com.healthio.core.data

import android.content.Context
import com.healthio.core.database.AppDatabase
import com.healthio.core.database.MealLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class MealRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.mealDao()

    suspend fun logMeal(meal: MealLog) {
        dao.insertMeal(meal)
    }

    fun getTodayCalories(): Flow<Int?> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return dao.getCaloriesBetween(startOfDay, endOfDay)
    }
    
    fun getTodayProtein(): Flow<Int?> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return dao.getProteinBetween(startOfDay, endOfDay)
    }
    
    fun getTodayCarbs(): Flow<Int?> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return dao.getCarbsBetween(startOfDay, endOfDay)
    }
    
    fun getTodayFat(): Flow<Int?> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return dao.getFatBetween(startOfDay, endOfDay)
    }

    fun getMealsSince(start: Long): Flow<List<MealLog>> {
        return dao.getMealsSince(start)
    }
}
