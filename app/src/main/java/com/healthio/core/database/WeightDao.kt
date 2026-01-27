package com.healthio.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightLog)

    @Update
    suspend fun updateWeight(weight: WeightLog)

    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestWeight(): Flow<WeightLog?>

    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC")
    fun getAllWeights(): Flow<List<WeightLog>>

    @Query("SELECT externalId FROM weight_logs WHERE externalId IS NOT NULL")
    suspend fun getImportedExternalIds(): List<String>

    @Query("SELECT * FROM weight_logs WHERE externalId = :externalId LIMIT 1")
    suspend fun getWeightByExternalId(externalId: String): WeightLog?
}
