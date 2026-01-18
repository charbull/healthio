package com.healthio.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String, // e.g., Running, Bike, Resistance, Other
    val calories: Int,
    val durationMinutes: Int,
    val source: String, // Manual or HealthConnect
    val externalId: String? = null, // ID from Health Connect to prevent duplicates
    val isSynced: Boolean = false
)
