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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
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
    val currentWeight: Float? = null,
    val weightUnit: String = "LBS",
    val carbsGoal: Int = 50,
    val fatGoal: Int = 130,
    val proteinGoal: Int = 56
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FastingRepository(application)
    private val mealRepository = MealRepository(application)
    private val workoutRepository = WorkoutRepository(application)
    private val weightRepository = WeightRepository(application)
    private val healthConnectManager = com.healthio.core.health.HealthConnectManager(application)
    private val calorieCalculator = com.healthio.core.domain.CalorieCalculator(healthConnectManager)
    private val context = application.applicationContext
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    private val targetFastDuration = Duration.ofHours(16).toMillis()

    init {
        viewModelScope.launch {
            val settingsFlow = context.dataStore.data.map { preferences ->
                SettingsData(
                    baseBurn = preferences[SettingsViewModel.BASE_DAILY_BURN] ?: 1800,
                    carbsGoal = preferences[SettingsViewModel.DAILY_CARBS_GOAL] ?: 50,
                    fatGoal = preferences[SettingsViewModel.DAILY_FAT_GOAL] ?: 130,
                    pMethod = preferences[SettingsViewModel.PROTEIN_CALC_METHOD] ?: "MULTIPLIER",
                    pFixed = preferences[SettingsViewModel.PROTEIN_FIXED_GOAL] ?: 150,
                    pMult = preferences[SettingsViewModel.PROTEIN_MULTIPLIER] ?: 0.8f,
                    weightUnit = preferences[SettingsViewModel.WEIGHT_UNIT] ?: "LBS"
                )
            }
            
            combine(
                repository.isFasting.onStart { emit(false) }, 
                repository.startTime.onStart { emit(null) },
                mealRepository.getTodayCalories().onStart { emit(null) },
                workoutRepository.getTodayWorkouts().onStart { emit(emptyList()) },
                settingsFlow.onStart { 
                    emit(SettingsData(1800, 50, 130, "MULTIPLIER", 150, 0.8f, "LBS")) 
                },
                mealRepository.getTodayProtein().onStart { emit(null) },
                mealRepository.getTodayCarbs().onStart { emit(null) },
                mealRepository.getTodayFat().onStart { emit(null) }
            ) { args ->
                val isFasting = args[0] as Boolean
                val startTime = args[1] as Long?
                val calories = args[2] as Int?
                val workouts = args[3] as List<com.healthio.core.database.WorkoutLog>
                val settings = args[4] as SettingsData
                val protein = args[5] as Int?
                val carbs = args[6] as Int?
                val fat = args[7] as Int?
                
                HomeData(
                    isFasting, startTime, calories, workouts, settings.baseBurn, protein, carbs, fat,
                    settings.carbsGoal, settings.fatGoal, settings.pMethod, settings.pFixed, settings.pMult,
                    settings.weightUnit
                )
            }.combine(weightRepository.getLatestWeight().onStart { emit(null) }.distinctUntilChanged()) { data, weight ->
                updateState(data, weight?.valueKg)
            }.collect { }
        }
        startTimer()
        startCalorieTimer()
    }

    private var calorieJob: Job? = null
    private fun startCalorieTimer() {
        calorieJob?.cancel()
        calorieJob = viewModelScope.launch {
            while (isActive) {
                refreshCalories()
                delay(300_000) // Refresh every 5 minutes
            }
        }
    }

    private suspend fun refreshCalories() {
        val data = lastHomeData ?: return
        val calendar = java.util.Calendar.getInstance()
        val dynamicBaseBurn = com.healthio.ui.stats.StatsUtils.calculateProRatedBMR(
            data.baseBurn,
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE),
            calendar.get(java.util.Calendar.SECOND)
        )

        val totalBurned = calorieCalculator.calculateTotalBurned(
            dynamicBaseBurn,
            data.workouts
        )

        _uiState.update { it.copy(todayBurnedCalories = totalBurned) }
    }

    private var lastHomeData: HomeData? = null
    private var lastRawWorkouts: List<com.healthio.core.database.WorkoutLog> = emptyList()

    private fun updateState(data: HomeData, weight: Float?) {
        lastHomeData = data
        lastRawWorkouts = data.workouts
        
        val currentWeightKg = weight ?: 70f
        
        val proteinGoal = if (data.pMethod == "FIXED") {
            data.pFixed
        } else {
            (data.pMult * currentWeightKg).toInt()
        }

        val calendar = java.util.Calendar.getInstance()
        val dynamicBaseBurn = com.healthio.ui.stats.StatsUtils.calculateProRatedBMR(
            data.baseBurn,
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE),
            calendar.get(java.util.Calendar.SECOND)
        )

        viewModelScope.launch {
            val totalBurned = calorieCalculator.calculateTotalBurned(
                dynamicBaseBurn,
                data.workouts
            )

            _uiState.update { currentState ->
                currentState.copy(
                    timerState = if (data.isFasting) TimerState.FASTING else TimerState.EATING,
                    startTime = data.startTime,
                    todayCalories = data.calories ?: 0,
                    todayBurnedCalories = totalBurned,
                    todayProtein = data.protein ?: 0,
                    todayCarbs = data.carbs ?: 0,
                    todayFat = data.fat ?: 0,
                    baseDailyBurn = data.baseBurn,
                    currentWeight = weight,
                    weightUnit = data.weightUnit,
                    carbsGoal = data.carbsGoal,
                    fatGoal = data.fatGoal,
                    proteinGoal = proteinGoal
                )
            }
            calculateProgress()
        }
    }

    data class SettingsData(
        val baseBurn: Int,
        val carbsGoal: Int,
        val fatGoal: Int,
        val pMethod: String,
        val pFixed: Int,
        val pMult: Float,
        val weightUnit: String
    )

    data class HomeData(
        val isFasting: Boolean, 
        val startTime: Long?, 
        val calories: Int?, 
        val workouts: List<com.healthio.core.database.WorkoutLog>, 
        val baseBurn: Int,
        val protein: Int?,
        val carbs: Int?,
        val fat: Int?,
        val carbsGoal: Int,
        val fatGoal: Int,
        val pMethod: String,
        val pFixed: Int,
        val pMult: Float,
        val weightUnit: String
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
        _uiState.update { currentState ->
            val now = System.currentTimeMillis()
            
            if (currentState.timerState == TimerState.FASTING && currentState.startTime != null) {
                val elapsed = now - currentState.startTime
                val progress = (elapsed.toFloat() / targetFastDuration).coerceIn(0f, 1f)
                val timeString = formatDuration(elapsed)
                
                currentState.copy(
                    progress = progress,
                    elapsedMillis = elapsed,
                    timeDisplay = timeString
                )
            } else {
                currentState.copy(
                    progress = 0f,
                    elapsedMillis = 0L,
                    timeDisplay = "00:00:00"
                )
            }
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

    fun logManualWeight(weightValue: Float) {
        val unit = _uiState.value.weightUnit
        val weightKg = if (unit == "LBS") {
            weightValue / 2.20462f
        } else {
            weightValue
        }
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
