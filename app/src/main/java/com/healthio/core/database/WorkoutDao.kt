package com.healthio.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutLog)

    @Query("SELECT * FROM workout_logs ORDER BY timestamp DESC")
    fun getAllWorkouts(): Flow<List<WorkoutLog>>

    @Query("SELECT SUM(calories) FROM workout_logs WHERE timestamp >= :start AND timestamp < :end")
    fun getBurnedCaloriesBetween(start: Long, end: Long): Flow<Int?>

    @Query("SELECT externalId FROM workout_logs WHERE externalId IS NOT NULL")
    suspend fun getImportedExternalIds(): List<String>

    @Query("SELECT * FROM workout_logs WHERE isSynced = 0")
    suspend fun getUnsyncedWorkouts(): List<WorkoutLog>

    @Query("UPDATE workout_logs SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM workout_logs WHERE timestamp >= :start AND timestamp < :end")
    suspend fun getCountBetween(start: Long, end: Long): Int

    @Query("SELECT * FROM workout_logs WHERE timestamp >= :start AND timestamp < :end")
    suspend fun getWorkoutsListBetween(start: Long, end: Long): List<WorkoutLog>

    @Query("SELECT * FROM workout_logs WHERE timestamp >= :start AND timestamp < :end")
    fun getWorkoutsFlowBetween(start: Long, end: Long): Flow<List<WorkoutLog>>

    @Query("DELETE FROM workout_logs WHERE externalId = :externalId")
    suspend fun deleteWorkoutByExternalId(externalId: String)
}
