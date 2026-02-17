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
import androidx.datastore.preferences.core.edit
import com.healthio.core.data.dataStore
import com.healthio.ui.settings.SettingsViewModel
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
                
                // 0. Sync BMR from Health Connect
                val todayStart = today.atStartOfDay(zoneId).toInstant()
                val bmr = healthConnectManager.fetchBasalMetabolicRate(todayStart, now)
                if (bmr > 0) {
                    getApplication<Application>().dataStore.edit { preferences ->
                        preferences[SettingsViewModel.BASE_DAILY_BURN] = bmr
                    }
                }
                
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

                    // 2. Fetch & Insert "Health Connect Active Burn"
                    val dayActiveCalories = healthConnectManager.fetchActiveCalories(dayStart, actualEnd)
                    
                    val activeBurnId = "hc_active_burn_${targetDate.year}_${targetDate.monthValue}_${targetDate.dayOfMonth}"
                    
                    // Clean up ALL old ID variants to prevent double counting
                    repository.deleteWorkoutByExternalId(activeBurnId)
                    repository.deleteWorkoutByExternalId("hc_daily_total_${targetDate.year}_${targetDate.monthValue}_${targetDate.dayOfMonth}")
                    repository.deleteWorkoutByExternalId("daily_active_burn_${targetDate.year}_${targetDate.monthValue}_${targetDate.dayOfMonth}")

                    if (dayActiveCalories > 0) {
                        repository.logWorkout(
                            WorkoutLog(
                                timestamp = dayStart.toEpochMilli() + 60000, // 1 min past midnight
                                type = "Health Connect Active Burn",
                                calories = dayActiveCalories,
                                durationMinutes = 0,
                                source = "Health Connect",
                                externalId = activeBurnId
                            )
                        )
                        totalDaysUpdated++
                    }
                }

                // 3. Sync Weights (Last 365 Days)
                val weightStart = today.minusDays(365).atStartOfDay(zoneId).toInstant()
                
                val weights = healthConnectManager.fetchWeights(weightStart, now)
                
                var addedWeightCount = 0
                var updatedWeightCount = 0
                
                weights.forEach { weight ->
                    val existing = weightRepository.getWeightByExternalId(weight.externalId)
                    if (existing == null) {
                        weightRepository.logWeight(
                            WeightLog(
                                timestamp = weight.timestamp.toEpochMilli(),
                                valueKg = weight.valueKg.toFloat(),
                                source = "Health Connect",
                                externalId = weight.externalId
                            )
                        )
                        addedWeightCount++
                    } else if (existing.valueKg != weight.valueKg.toFloat() || existing.timestamp != weight.timestamp.toEpochMilli()) {
                        weightRepository.updateWeight(
                            existing.copy(
                                timestamp = weight.timestamp.toEpochMilli(),
                                valueKg = weight.valueKg.toFloat()
                            )
                        )
                        updatedWeightCount++
                    }
                }

                // Result Message
                val parts = mutableListOf<String>()
                if (totalNewWorkouts > 0) parts.add("$totalNewWorkouts workouts")
                if (totalDaysUpdated > 0) parts.add("active calories updated for $totalDaysUpdated days")
                if (weights.isNotEmpty()) {
                    val msg = if (updatedWeightCount > 0) "$addedWeightCount new, $updatedWeightCount updated weights" else "$addedWeightCount new weights"
                    parts.add("$msg (found ${weights.size})")
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