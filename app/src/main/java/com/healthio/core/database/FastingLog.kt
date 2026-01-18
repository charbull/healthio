package com.healthio.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fasting_logs")
data class FastingLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val durationMillis: Long,
    val isSynced: Boolean = false
)
