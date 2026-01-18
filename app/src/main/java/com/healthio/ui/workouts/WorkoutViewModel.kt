package com.healthio.ui.workouts

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthio.core.database.WorkoutLog
import com.healthio.core.health.HealthConnectManager
import com.healthio.core.data.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

sealed class WorkoutSyncState {
    object Idle : WorkoutSyncState()
    object Syncing : WorkoutSyncState()
    data class Success(val count: Int) : WorkoutSyncState()
    data class Error(val message: String) : WorkoutSyncState()
}

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(application)
    private val healthManager = HealthConnectManager(application)
    private val context = application.applicationContext

    private val _syncState = MutableStateFlow<WorkoutSyncState>(WorkoutSyncState.Idle)
    val syncState: StateFlow<WorkoutSyncState> = _syncState.asStateFlow()

    fun fetchFromHealthConnect() {
        viewModelScope.launch {
            _syncState.value = WorkoutSyncState.Syncing
            val sdkStatus = HealthConnectClient.getSdkStatus(context)
            android.util.Log.d("Healthio", "Health Connect SDK Status: $sdkStatus")
            
            if (sdkStatus == HealthConnectClient.SDK_UNAVAILABLE) {
                _syncState.value = WorkoutSyncState.Error("Health Connect is NOT installed. Please install it to sync Garmin data.")
                return@launch
            }
            
            if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
                _syncState.value = WorkoutSyncState.Error("Health Connect is not ready (Status: $sdkStatus)")
                return@launch
            }

            android.util.Log.d("Healthio", "Starting Health Connect Fetch...")
            try {
                if (!healthManager.hasPermissions()) {
                    android.util.Log.w("Healthio", "Permissions missing!")
                    _syncState.value = WorkoutSyncState.Error("Missing Health Connect Permissions")
                    return@launch
                }

                val now = Instant.now()
                val startTime = now.minus(7, ChronoUnit.DAYS)
                android.util.Log.d("Healthio", "Fetching records since $startTime")
                
                val hcWorkouts = healthManager.fetchWorkouts(startTime, now)
                android.util.Log.d("Healthio", "Found ${hcWorkouts.size} sessions in Health Connect")
                
                if (hcWorkouts.isEmpty()) {
                    _syncState.value = WorkoutSyncState.Success(0)
                    return@launch
                }

                val existingIds = repository.getImportedExternalIds()
                val newWorkouts = hcWorkouts.filter { it.externalId !in existingIds }
                android.util.Log.d("Healthio", "New workouts to import: ${newWorkouts.size}")

                newWorkouts.forEach { hc ->
                    repository.logWorkout(
                        WorkoutLog(
                            timestamp = hc.startTime.toEpochMilli(),
                            type = mapExerciseType(hc.type),
                            calories = hc.calories,
                            durationMinutes = hc.durationMinutes,
                            source = "HealthConnect",
                            externalId = hc.externalId
                        )
                    )
                }

                _syncState.value = WorkoutSyncState.Success(newWorkouts.size)
            } catch (e: Exception) {
                android.util.Log.e("Healthio", "Sync error", e)
                _syncState.value = WorkoutSyncState.Error("Sync Error: ${e.localizedMessage}")
            }
        }
    }

    fun logManualWorkout(type: String, duration: Int, calories: Int) {
        viewModelScope.launch {
            repository.logWorkout(
                WorkoutLog(
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    calories = calories,
                    durationMinutes = duration,
                    source = "Manual"
                )
            )
        }
    }

    fun resetSyncState() {
        _syncState.value = WorkoutSyncState.Idle
    }

    private fun mapExerciseType(type: Int): String {
        return when (type) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Running"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Biking"
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Resistance"
            else -> "Other"
        }
    }
}