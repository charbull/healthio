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

sealed class WorkoutSyncState {
    object Idle : WorkoutSyncState()
    data class Success(val message: String) : WorkoutSyncState()
    data class Error(val message: String) : WorkoutSyncState()
}

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(application)

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

    fun resetSyncState() {
        _syncState.value = WorkoutSyncState.Idle
    }
}