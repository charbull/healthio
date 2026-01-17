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
    // We'll just get all for now and filter in UI/Repo for simplicity or use a generic query
    @Query("SELECT * FROM fasting_logs ORDER BY endTime DESC")
    fun getAllLogs(): Flow<List<FastingLog>>
}
