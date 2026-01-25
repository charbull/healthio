package com.healthio.core.data

import android.content.Context
import com.healthio.core.database.AppDatabase
import com.healthio.core.database.WorkoutLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class WorkoutRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.workoutDao()

    suspend fun logWorkout(workout: WorkoutLog) {
        dao.insertWorkout(workout)
    }

    fun getTodayBurnedCalories(): Flow<Int?> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return dao.getBurnedCaloriesBetween(startOfDay, endOfDay)
    }

    suspend fun getImportedExternalIds(): List<String> {
        return dao.getImportedExternalIds()
    }

    suspend fun getWorkoutsBetween(start: Long, end: Long): List<WorkoutLog> {
        return dao.getWorkoutsListBetween(start, end)
    }

    suspend fun deleteWorkoutByExternalId(externalId: String) {
        dao.deleteWorkoutByExternalId(externalId)
    }
}
