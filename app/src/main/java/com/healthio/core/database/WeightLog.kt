package com.healthio.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_logs")
data class WeightLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val valueKg: Float,
    val source: String = "Manual", // Manual or Health Connect
    val externalId: String? = null,
    val isSynced: Boolean = false
)
