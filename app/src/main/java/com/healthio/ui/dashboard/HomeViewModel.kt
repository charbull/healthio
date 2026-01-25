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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.Locale

import com.healthio.core.data.MealRepository
import com.healthio.core.data.WorkoutRepository
import com.healthio.core.data.WeightRepository

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
    val todayProtein: Int = 0,
    val todayCarbs: Int = 0,
    val todayFat: Int = 0,
    val baseDailyBurn: Int = 1800,
    val currentWeight: Float? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FastingRepository(application)
    private val mealRepository = MealRepository(application)
    private val workoutRepository = WorkoutRepository(application)
    private val weightRepository = WeightRepository(application)
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
                baseBurnFlow,
                mealRepository.getTodayProtein(),
                mealRepository.getTodayCarbs(),
                mealRepository.getTodayFat()
            ) { args ->
                val isFasting = args[0] as Boolean
                val startTime = args[1] as Long?
                val calories = args[2] as Int?
                val burned = args[3] as Int?
                val baseBurn = args[4] as Int
                val protein = args[5] as Int?
                val carbs = args[6] as Int?
                val fat = args[7] as Int?
                
                HomeData(isFasting, startTime, calories, burned, baseBurn, protein, carbs, fat)
            }.combine(weightRepository.getLatestWeight().onStart { emit(null) }) { data, weight ->
                updateState(data, weight?.valueKg)
            }.collect { }
        }
        startTimer()
    }

    private var lastActiveBurned: Int = 0

    private fun updateState(data: HomeData, weight: Float?) {
        lastActiveBurned = data.burned ?: 0
        val calendar = java.util.Calendar.getInstance()
        val hoursPassed = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minutesPassed = calendar.get(java.util.Calendar.MINUTE)
        val dayProgress = (hoursPassed * 60 + minutesPassed) / 1440f
        
        val dynamicBaseBurn = (data.baseBurn * dayProgress).toInt()

        _uiState.value = _uiState.value.copy(
            timerState = if (data.isFasting) TimerState.FASTING else TimerState.EATING,
            startTime = data.startTime,
            todayCalories = data.calories ?: 0,
            todayBurnedCalories = lastActiveBurned + dynamicBaseBurn,
            todayProtein = data.protein ?: 0,
            todayCarbs = data.carbs ?: 0,
            todayFat = data.fat ?: 0,
            baseDailyBurn = data.baseBurn,
            currentWeight = weight
        )
        calculateProgress()
    }

    data class HomeData(
        val isFasting: Boolean, 
        val startTime: Long?, 
        val calories: Int?, 
        val burned: Int?, 
        val baseBurn: Int,
        val protein: Int?,
        val carbs: Int?,
        val fat: Int?
    )

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
                timeDisplay = "00:00:00",
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

    fun logManualMeal(name: String, calories: Int, protein: Int, carbs: Int, fat: Int) {
        viewModelScope.launch {
            mealRepository.logMeal(
                com.healthio.core.database.MealLog(
                    timestamp = System.currentTimeMillis(),
                    foodName = name,
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat
                )
            )
        }
    }

    fun logManualWeight(weightLbs: Float) {
        val weightKg = weightLbs / 2.20462f
        viewModelScope.launch {
            weightRepository.logWeight(
                com.healthio.core.database.WeightLog(
                    timestamp = System.currentTimeMillis(),
                    valueKg = weightKg,
                    source = "manual"
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
