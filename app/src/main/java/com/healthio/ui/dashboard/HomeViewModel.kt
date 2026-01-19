package com.healthio.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthio.core.data.FastingRepository
import com.healthio.core.data.QuotesRepository
import com.healthio.core.data.dataStore
import com.healthio.ui.settings.SettingsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.Locale

import com.healthio.core.data.MealRepository
import com.healthio.core.data.WorkoutRepository

enum class TimerState {
    FASTING, EATING
}

data class HomeUiState(
    val timerState: TimerState = TimerState.FASTING,
    val progress: Float = 0f,
    val elapsedMillis: Long = 0L,
    val timeDisplay: String = "00:00:00",
    val startTime: Long? = null,
    val showFeedbackDialog: Boolean = false,
    val completedDuration: String = "",
    val feedbackQuote: String = "",
    val todayCalories: Int = 0,
    val todayBurnedCalories: Int = 0,
    val baseDailyBurn: Int = 1800
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FastingRepository(application)
    private val mealRepository = MealRepository(application)
    private val workoutRepository = WorkoutRepository(application)
    private val context = application.applicationContext
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    private val targetFastDuration = Duration.ofHours(16).toMillis()

    init {
        viewModelScope.launch {
            val baseBurnFlow = context.dataStore.data.map { it[SettingsViewModel.BASE_DAILY_BURN] ?: 1800 }
            
            combine(
                repository.isFasting, 
                repository.startTime,
                mealRepository.getTodayCalories(),
                workoutRepository.getTodayBurnedCalories(),
                baseBurnFlow
            ) { isFasting, startTime, calories, burned, baseBurn ->
                Pentuple(isFasting, startTime, calories, burned, baseBurn)
            }.collect { (isFasting, startTime, calories, burned, baseBurn) ->
                updateState(isFasting, startTime, calories, burned, baseBurn)
            }
        }
        startTimer()
    }

    private var lastActiveBurned: Int = 0

    private fun updateState(isFasting: Boolean, startTime: Long?, calories: Int?, burned: Int?, baseBurn: Int) {
        lastActiveBurned = burned ?: 0
        val calendar = java.util.Calendar.getInstance()
        val hoursPassed = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minutesPassed = calendar.get(java.util.Calendar.MINUTE)
        val dayProgress = (hoursPassed * 60 + minutesPassed) / 1440f
        
        val dynamicBaseBurn = (baseBurn * dayProgress).toInt()

        _uiState.value = _uiState.value.copy(
            timerState = if (isFasting) TimerState.FASTING else TimerState.EATING,
            startTime = startTime,
            todayCalories = calories ?: 0,
            todayBurnedCalories = lastActiveBurned + dynamicBaseBurn,
            baseDailyBurn = baseBurn
        )
        calculateProgress()
    }

    data class Pentuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                calculateProgress()
                delay(1000)
            }
        }
    }

    private fun calculateProgress() {
        val currentState = _uiState.value
        val now = System.currentTimeMillis()
        
        // Calculate dynamic BMR burn
        val calendar = java.util.Calendar.getInstance()
        val hoursPassed = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minutesPassed = calendar.get(java.util.Calendar.MINUTE)
        val secondsPassed = calendar.get(java.util.Calendar.SECOND)
        val dayProgress = (hoursPassed * 3600 + minutesPassed * 60 + secondsPassed) / 86400f
        
        val dynamicBaseBurn = (currentState.baseDailyBurn * dayProgress).toInt()
        val totalBurned = lastActiveBurned + dynamicBaseBurn

        if (currentState.timerState == TimerState.FASTING && currentState.startTime != null) {
            val elapsed = now - currentState.startTime
            val progress = (elapsed.toFloat() / targetFastDuration).coerceIn(0f, 1f)
            val timeString = formatDuration(elapsed)
            
            _uiState.value = currentState.copy(
                progress = progress,
                elapsedMillis = elapsed,
                timeDisplay = timeString,
                todayBurnedCalories = totalBurned
            )
        } else {
            _uiState.value = currentState.copy(
                progress = 0f,
                elapsedMillis = 0L,
                timeDisplay = "Ready to Fast",
                todayBurnedCalories = totalBurned
            )
        }
    }

    private fun formatDuration(millis: Long): String {
        val duration = Duration.ofMillis(millis)
        return String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            duration.toHours(),
            duration.toMinutesPart(),
            duration.toSecondsPart()
        )
    }

    fun startFastNow() {
        viewModelScope.launch {
            repository.startFast(System.currentTimeMillis())
        }
    }

    fun startFastAt(timestamp: Long) {
        val clampedTime = if (timestamp > System.currentTimeMillis()) System.currentTimeMillis() else timestamp
        viewModelScope.launch {
            repository.startFast(clampedTime)
        }
    }

    fun requestEndFast() {
        val currentState = _uiState.value
        val now = System.currentTimeMillis()
        val elapsed = if (currentState.startTime != null) now - currentState.startTime else 0L
        
        _uiState.value = currentState.copy(
            showFeedbackDialog = true,
            completedDuration = formatDuration(elapsed),
            feedbackQuote = QuotesRepository.getRandomQuote()
        )
    }

    fun confirmEndFast() {
        val currentState = _uiState.value
        val now = System.currentTimeMillis()
        val startTime = currentState.startTime ?: return

        viewModelScope.launch {
            repository.logCompletedFast(startTime, now)
            repository.endFast()
            _uiState.value = _uiState.value.copy(showFeedbackDialog = false)
        }
    }

    fun logPastFast(startTime: Long, endTime: Long) {
        if (endTime <= startTime) return
        viewModelScope.launch {
            repository.logCompletedFast(startTime, endTime)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
