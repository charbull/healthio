package com.healthio.core.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.WeightRecord
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

data class HCWeight(
    val externalId: String,
    val timestamp: Instant,
    val valueKg: Double
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
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )

    suspend fun hasPermissions(): Boolean {
        val client = getClient() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun fetchActiveCalories(startTime: Instant, endTime: Instant): Int {
        val client = getClient() ?: return 0
        if (!hasPermissions()) return 0
        
        val request = ReadRecordsRequest(
            recordType = ActiveCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        return client.readRecords(request).records
            .sumOf { it.energy.inKilocalories.toInt() }
    }

    suspend fun fetchTotalCalories(startTime: Instant, endTime: Instant): Int {
        val client = getClient() ?: return 0
        if (!hasPermissions()) return 0
        
        val request = ReadRecordsRequest(
            recordType = TotalCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        return client.readRecords(request).records
            .sumOf { it.energy.inKilocalories.toInt() }
    }

    suspend fun fetchBasalMetabolicRate(startTime: Instant, endTime: Instant): Int {
        val client = getClient() ?: return 0
        if (!hasPermissions()) return 0
        
        val request = ReadRecordsRequest(
            recordType = BasalMetabolicRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        // BMR is usually a single record per day or period
        return client.readRecords(request).records
            .maxOfOrNull { it.basalMetabolicRate.inKilocaloriesPerDay.toInt() } ?: 0
            // Note: BMR is a rate, taking max avoids summing duplicates
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

    suspend fun fetchWeights(startTime: Instant, endTime: Instant): List<HCWeight> {
        val client = getClient() ?: return emptyList()
        if (!hasPermissions()) return emptyList()

        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        return client.readRecords(request).records
            .sortedByDescending { it.time }
            .map { record ->
                HCWeight(
                    externalId = record.metadata.id,
                    timestamp = record.time,
                    valueKg = record.weight.inKilograms
                )
            }
    }
}
