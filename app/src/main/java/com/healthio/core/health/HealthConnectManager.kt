package com.healthio.core.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant

data class HCWorkout(
    val externalId: String,
    val startTime: Instant,
    val type: Int,
    val durationMinutes: Int,
    val calories: Int
)

class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    suspend fun hasPermissions(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun fetchWorkouts(startTime: Instant, endTime: Instant): List<HCWorkout> {
        if (!hasPermissions()) return emptyList()
        
        val sessionRequest = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        val sessions = healthConnectClient.readRecords(sessionRequest).records
        
        return sessions.map { session ->
            // Try to find calories for this session's time range
            val calorieRequest = ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
            )
            val calories = healthConnectClient.readRecords(calorieRequest).records
                .sumOf { it.energy.inKilocalories.toInt() }
            
            HCWorkout(
                externalId = session.metadata.id,
                startTime = session.startTime,
                type = session.exerciseType,
                durationMinutes = Duration.between(session.startTime, session.endTime).toMinutes().toInt(),
                calories = calories
            )
        }
    }
}