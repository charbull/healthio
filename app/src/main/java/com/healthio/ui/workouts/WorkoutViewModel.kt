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
                val now = Instant.now()
                
                var totalNewWorkouts = 0
                var totalDaysUpdated = 0

                // Sync last 30 days of activity (Day by Day)
                for (i in 0..29) {
                    val targetDate = today.minusDays(i.toLong())
                    val dayStart = targetDate.atStartOfDay(zoneId).toInstant()
                    // For today, end at 'now'. For past days, end of day.
                    val dayEnd = targetDate.plusDays(1).atStartOfDay(zoneId).toInstant()
                    val actualEnd = if (i == 0) now else dayEnd
                    
                    if (actualEnd.isBefore(dayStart)) continue

                    // 1. Fetch & Insert Workouts for this day
                    val dayWorkouts = healthConnectManager.fetchWorkouts(dayStart, actualEnd)
                    // Get ALL existing workouts for this day from DB to check dups and calc sum
                    val dbWorkouts = repository.getWorkoutsBetween(dayStart.toEpochMilli(), actualEnd.toEpochMilli())
                    val existingExternalIds = dbWorkouts.mapNotNull { it.externalId }.toSet()

                    var dayNewCount = 0
                    dayWorkouts.forEach { workout ->
                        if (workout.externalId !in existingExternalIds) {
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
                            dayNewCount++
                        }
                    }
                    totalNewWorkouts += dayNewCount

                    // 2. Calculate Active Burn Adjustment for this day
                    var dayTotalActive = healthConnectManager.fetchActiveCalories(dayStart, actualEnd)
                    
                    // Fallback logic
                    if (dayTotalActive == 0) {
                        val total = healthConnectManager.fetchTotalCalories(dayStart, actualEnd)
                        if (total > 0) dayTotalActive = total / 4
                    }

                    if (dayTotalActive > 0) {
                        // Refresh DB list to include any just-inserted workouts
                        // Optimization: just assume dbWorkouts + inserted ones
                        // But safer to re-query or calc manually. 
                        // Let's re-query to be safe and consistent
                        val updatedDbWorkouts = repository.getWorkoutsBetween(dayStart.toEpochMilli(), actualEnd.toEpochMilli())
                        
                        val knownSessionCalories = updatedDbWorkouts
                            .filter { it.type != "Daily Active Burn" }
                            .sumOf { it.calories }
                        
                        val adjustment = (dayTotalActive - knownSessionCalories).coerceAtLeast(0)
                        
                        // Always upsert the adjustment if valid, or if we need to 'zero out' an old incorrect one?
                        // If adjustment is 0, we might want to overwrite an old value if it existed? 
                        // For now, only insert if > 0.
                        if (adjustment > 0) {
                            val adjId = "daily_active_burn_${targetDate.year}_${targetDate.monthValue}_${targetDate.dayOfMonth}"
                            repository.logWorkout(
                                WorkoutLog(
                                    timestamp = dayStart.toEpochMilli() + 60000, // 1 min past midnight
                                    type = "Daily Active Burn",
                                    calories = adjustment,
                                    durationMinutes = 0,
                                    source = "Health Connect",
                                    externalId = adjId
                                )
                            )
                            totalDaysUpdated++
                        }
                    }
                }

                // 3. Sync Weights (Last 365 Days)
                val weightStart = today.minusDays(365).atStartOfDay(zoneId).toInstant()
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
                if (totalNewWorkouts > 0) parts.add("$totalNewWorkouts workouts")
                if (totalDaysUpdated > 0) parts.add("active calories updated for $totalDaysUpdated days")
                if (weights.isNotEmpty()) {
                    parts.add("$addedWeightCount new weights (found ${weights.size})")
                } else {
                    parts.add("0 weights found")
                }
                
                if (parts.isNotEmpty()) {
                    _syncState.value = WorkoutSyncState.Success("Synced: ${parts.joinToString(", ")}")
                } else {
                    _syncState.value = WorkoutSyncState.Success("Sync finished: No new data")
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