package com.healthio.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FastingDao {
    @Insert
    suspend fun insertLog(log: FastingLog)

    // Get logs for the last X days.
    @Query("SELECT * FROM fasting_logs ORDER BY endTime DESC")
    fun getAllLogs(): Flow<List<FastingLog>>

    @Query("SELECT * FROM fasting_logs WHERE isSynced = 0")
    suspend fun getUnsyncedLogs(): List<FastingLog>

    @Query("UPDATE fasting_logs SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
}
