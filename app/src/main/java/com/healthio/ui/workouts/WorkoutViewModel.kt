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
    object NeedsPermission : WorkoutSyncState()
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
            
            val status = healthManager.getSdkStatus()
            android.util.Log.d("Healthio", "Health Connect SDK Status: $status")
            
            if (status != HealthConnectClient.SDK_AVAILABLE) {
                val msg = when (status) {
                    HealthConnectClient.SDK_UNAVAILABLE -> "NOT installed (Code 1)"
                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Update Required (Code 2)"
                    else -> "SDK Not Ready (Code $status)"
                }
                _syncState.value = WorkoutSyncState.Error(msg)
                return@launch
            }

            try {
                if (!healthManager.hasPermissions()) {
                    android.util.Log.d("Healthio", "Permissions missing, setting NeedsPermission state")
                    _syncState.value = WorkoutSyncState.NeedsPermission
                    return@launch
                }

                android.util.Log.d("Healthio", "Permissions present, starting fetch")
                val now = Instant.now()
                val startTime = now.minus(7, ChronoUnit.DAYS)
                val hcWorkouts = healthManager.fetchWorkouts(startTime, now)
                
                val existingIds = repository.getImportedExternalIds()
                val newWorkouts = hcWorkouts.filter { it.externalId !in existingIds }

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
