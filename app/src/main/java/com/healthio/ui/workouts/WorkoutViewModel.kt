package com.healthio.ui.workouts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthio.core.database.WorkoutLog
import com.healthio.core.data.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.healthio.core.health.HealthConnectManager
import androidx.health.connect.client.HealthConnectClient

sealed class WorkoutSyncState {
    object Idle : WorkoutSyncState()
    object Loading : WorkoutSyncState()
    object PermissionRequired : WorkoutSyncState()
    data class Success(val message: String) : WorkoutSyncState()
    data class Error(val message: String) : WorkoutSyncState()
}

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(application)
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
                val startOfDay = today.atStartOfDay(zoneId).toInstant()
                val now = Instant.now()

                val workouts = healthConnectManager.fetchWorkouts(startOfDay, now)
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

                // New Logic: Pull total active calories for the day
                val totalActiveBurn = healthConnectManager.fetchActiveCalories(startOfDay, now)
                
                // We create/update a special entry for "Daily Active Burn" to capture non-session movement
                val dailyAdjustmentId = "daily_active_burn_${today.year}_${today.monthValue}_${today.dayOfMonth}"
                
                // Calculate how much active burn is NOT yet in our DB for today
                // For simplicity, we'll just log the "Daily Adjustment" as the total active burn minus the sum of other imported workouts
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

                if (addedWorkoutsCount > 0 || adjustmentValue > 0) {
                    val msg = if (addedWorkoutsCount > 0) "Imported $addedWorkoutsCount workouts and daily activity" else "Daily active burn updated"
                    _syncState.value = WorkoutSyncState.Success(msg)
                } else {
                    _syncState.value = WorkoutSyncState.Success("No new data found for today")
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