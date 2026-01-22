package com.healthio.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightLog)

    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestWeight(): Flow<WeightLog?>

    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC")
    fun getAllWeights(): Flow<List<WeightLog>>

    @Query("SELECT externalId FROM weight_logs WHERE externalId IS NOT NULL")
    suspend fun getImportedExternalIds(): List<String>
}
