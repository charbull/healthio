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
    
    fun getSdkStatus(): Int {
        return HealthConnectClient.getSdkStatus(context)
    }

    private fun getClient(): HealthConnectClient? {
        return if (getSdkStatus() == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    suspend fun hasPermissions(): Boolean {
        val client = getClient() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun fetchWorkouts(startTime: Instant, endTime: Instant): List<HCWorkout> {
        val client = getClient() ?: return emptyList()
        if (!hasPermissions()) return emptyList()
        
        val sessionRequest = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        val sessions = client.readRecords(sessionRequest).records
        
        return sessions.map { session ->
            val calorieRequest = ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
            )
            val calories = client.readRecords(calorieRequest).records
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
