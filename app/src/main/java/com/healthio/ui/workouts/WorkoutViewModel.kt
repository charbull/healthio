package com.healthio.ui.workouts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthio.core.database.WeightLog
import com.healthio.core.database.WorkoutLog
import com.healthio.core.data.WeightRepository
import com.healthio.core.data.WorkoutRepository
import com.healthio.core.health.HealthConnectManager
import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed class WorkoutSyncState {
    object Idle : WorkoutSyncState()
    object Loading : WorkoutSyncState()
    object PermissionRequired : WorkoutSyncState()
    data class Success(val message: String) : WorkoutSyncState()
    data class Error(val message: String) : WorkoutSyncState()
}

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(application)
    private val weightRepository = WeightRepository(application)
    private val healthConnectManager = HealthConnectManager(application)

    private val _syncState = MutableStateFlow<WorkoutSyncState>(WorkoutSyncState.Idle)
    val syncState: StateFlow<WorkoutSyncState> = _syncState.asStateFlow()

    fun logManualWorkout(type: String, duration: Int, calories: Int, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.logWorkout(
                WorkoutLog(
                    timestamp = timestamp,
                    type = type,
                    calories = calories,
                    durationMinutes = duration,
                    source = "Manual"
                )
            )
            _syncState.value = WorkoutSyncState.Success("Workout Saved")
        }
    }

    fun syncFromHealthConnect() {
        viewModelScope.launch {
            _syncState.value = WorkoutSyncState.Loading
            
            val status = healthConnectManager.getSdkStatus()
            if (status != HealthConnectClient.SDK_AVAILABLE) {
                val errorMsg = when (status) {
                    HealthConnectClient.SDK_UNAVAILABLE -> "Health Connect is not supported or not installed."
                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Update required for Health Connect."
                    else -> "Health Connect unavailable ($status)"
                }
                _syncState.value = WorkoutSyncState.Error(errorMsg)
                return@launch
            }

            if (!healthConnectManager.hasPermissions()) {
                _syncState.value = WorkoutSyncState.PermissionRequired
                return@launch
            }

            try {
                val zoneId = ZoneId.systemDefault()
                val today = LocalDate.now(zoneId)
                
                // Scan last 7 days to catch up on missed logs
                val startOfScan = today.minusDays(7).atStartOfDay(zoneId).toInstant()
                val now = Instant.now()

                // 1. Sync Workouts
                val workouts = healthConnectManager.fetchWorkouts(startOfScan, now)
                val existingIds = repository.getImportedExternalIds().toSet()

                var addedWorkoutsCount = 0
                workouts.forEach { workout ->
                    if (workout.externalId !in existingIds) {
                        repository.logWorkout(
                            WorkoutLog(
                                timestamp = workout.startTime.toEpochMilli(),
                                type = "Imported (Type ${workout.type})",
                                calories = workout.calories,
                                durationMinutes = workout.durationMinutes,
                                source = "Health Connect",
                                externalId = workout.externalId
                            )
                        )
                        addedWorkoutsCount++
                    }
                }

                // 2. Sync Active Calories (Daily Burn)
                val startOfToday = today.atStartOfDay(zoneId).toInstant()
                var totalActiveBurn = healthConnectManager.fetchActiveCalories(startOfToday, now)
                
                // Fallback: If active burn is 0, check Total Calories
                if (totalActiveBurn == 0) {
                    val totalBurn = healthConnectManager.fetchTotalCalories(startOfToday, now)
                    if (totalBurn > 0) {
                        // Estimate active burn as Total - partial BMR (very rough estimate for debug)
                        totalActiveBurn = (totalBurn / 4) 
                    }
                }
                
                val dailyAdjustmentId = "daily_active_burn_${today.year}_${today.monthValue}_${today.dayOfMonth}"
                val workoutSessionsBurn = workouts.sumOf { it.calories }
                val adjustmentValue = (totalActiveBurn - workoutSessionsBurn).coerceAtLeast(0)

                if (adjustmentValue > 0) {
                    repository.logWorkout(
                        WorkoutLog(
                            timestamp = now.toEpochMilli(),
                            type = "Daily Active Burn",
                            calories = adjustmentValue,
                            durationMinutes = 0,
                            source = "Health Connect",
                            externalId = dailyAdjustmentId
                        )
                    )
                }

                // 3. Sync Weights
                // Scan last 30 days for weight
                val weightStart = today.minusDays(30).atStartOfDay(zoneId).toInstant()
                val weights = healthConnectManager.fetchWeights(weightStart, now)
                val existingWeightIds = weightRepository.getImportedExternalIds().toSet()
                
                var addedWeightCount = 0
                weights.forEach { weight ->
                    if (weight.externalId !in existingWeightIds) {
                        weightRepository.logWeight(
                            WeightLog(
                                timestamp = weight.timestamp.toEpochMilli(),
                                valueKg = weight.valueKg.toFloat(),
                                source = "Health Connect",
                                externalId = weight.externalId
                            )
                        )
                        addedWeightCount++
                    }
                }

                // Result Message
                val parts = mutableListOf<String>()
                if (addedWorkoutsCount > 0) parts.add("$addedWorkoutsCount workouts")
                if (adjustmentValue > 0) parts.add("Active Calorie update")
                if (weights.isNotEmpty()) {
                    parts.add("$addedWeightCount new weights (found ${weights.size})")
                } else {
                    parts.add("0 weights found")
                }
                
                if (parts.isNotEmpty()) {
                    _syncState.value = WorkoutSyncState.Success("Imported: ${parts.joinToString(", ")}")
                } else {
                    _syncState.value = WorkoutSyncState.Success("No new data found")
                }
            } catch (e: Exception) {
                _syncState.value = WorkoutSyncState.Error("Sync failed: ${e.message}")
            }
        }
    }

    fun resetSyncState() {
        _syncState.value = WorkoutSyncState.Idle
    }
}
