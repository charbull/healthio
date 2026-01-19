package com.healthio.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthio.core.data.FastingRepository
import com.healthio.core.database.AppDatabase
import com.healthio.core.database.FastingLog
import com.healthio.core.database.MealLog
import com.healthio.core.database.WorkoutLog
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val fastingRepository = FastingRepository(application)
    private val db = AppDatabase.getDatabase(application)
    private val workoutDao = db.workoutDao()
    private val mealDao = db.mealDao()

    private val _timeRange = MutableStateFlow(TimeRange.Week)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    data class FastingStatsState(
        val chartSeries: List<List<ChartEntry>> = emptyList(),
        val summaryValue: String = ""
    )
    
    data class EnergyStatsState(
        val chartSeries: List<List<ChartEntry>> = emptyList(), // Series 1: Intake, Series 2: Burned
        val workoutSummary: WorkoutSummary? = null
    )
    
    data class NutritionStatsState(
        val chartSeries: List<List<ChartEntry>> = emptyList(), // P, C, F
        val summaryValue: String = ""
    )

    private val _fastingState = MutableStateFlow(FastingStatsState())
    val fastingState: StateFlow<FastingStatsState> = _fastingState.asStateFlow()

    private val _energyState = MutableStateFlow(EnergyStatsState())
    val energyState: StateFlow<EnergyStatsState> = _energyState.asStateFlow()

    private val _nutritionState = MutableStateFlow(NutritionStatsState())
    val nutritionState: StateFlow<NutritionStatsState> = _nutritionState.asStateFlow()

    private val _chartLabels = MutableStateFlow<List<String>>(emptyList())
    val chartLabels: StateFlow<List<String>> = _chartLabels.asStateFlow()

    data class WorkoutSummary(
        val sessions: Int, 
        val calories: Int, 
        val minutes: Int,
        val sessionsWeek: Int = 0,
        val sessionsMonth: Int = 0,
        val sessionsYear: Int = 0
    )

    private var allFastingLogs: List<FastingLog> = emptyList()
    private var allWorkoutLogs: List<WorkoutLog> = emptyList()
    private var allMealLogs: List<MealLog> = emptyList()

    init {
        viewModelScope.launch {
            fastingRepository.fastingHistory.collect { history ->
                allFastingLogs = history
                updateChartData()
            }
        }
        viewModelScope.launch {
            workoutDao.getAllWorkouts().collect { workouts ->
                allWorkoutLogs = workouts
                updateChartData()
            }
        }
        viewModelScope.launch {
            mealDao.getAllMeals().collect { meals ->
                allMealLogs = meals
                updateChartData()
            }
        }
    }

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
        updateChartData()
    }


    private fun updateChartData() {
        val range = _timeRange.value
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        
        val labels = when (range) {
            TimeRange.Week -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            TimeRange.Month -> (1..YearMonth.now(zoneId).lengthOfMonth()).map { it.toString() }
            TimeRange.Year -> listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        }
        val bucketCount = labels.size

        // 1. Fasting
        val fastingDailyTotal = mutableMapOf<Int, Float>()
        allFastingLogs.forEach { log ->
            var currentStart = Instant.ofEpochMilli(log.startTime).atZone(zoneId)
            val endDateTime = Instant.ofEpochMilli(log.endTime).atZone(zoneId)
            while (currentStart.isBefore(endDateTime)) {
                val endOfDay = currentStart.toLocalDate().plusDays(1).atStartOfDay(zoneId)
                val segmentEnd = if (endDateTime.isBefore(endOfDay)) endDateTime else endOfDay
                val (index, include) = StatsUtils.getBucketIndex(currentStart.toLocalDate(), range, today)
                if (include) {
                    val durationHrs = ChronoUnit.MILLIS.between(currentStart, segmentEnd) / 3600000f
                    fastingDailyTotal[index] = (fastingDailyTotal[index] ?: 0f) + durationHrs
                }
                currentStart = segmentEnd
            }
        }
        _fastingState.value = FastingStatsState(
            chartSeries = listOf((1..bucketCount).map { entryOf(it - 1, fastingDailyTotal[it] ?: 0f) }),
            summaryValue = "Active consistency"
        )

        // 2. Energy & Workouts
        val energyIntake = mutableMapOf<Int, Float>()
        val energyBurned = mutableMapOf<Int, Float>() // From workouts only for now
        
        // Intake
        allMealLogs.filterMealsIn(range, today).forEach { log ->
            val (index, _) = StatsUtils.getBucketIndex(Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate(), range, today)
            energyIntake[index] = (energyIntake[index] ?: 0f) + log.calories.toFloat()
        }
        
        // Burned (Workouts)
        val filteredWorkouts = allWorkoutLogs.filterWorkoutsIn(range, today)
        filteredWorkouts.forEach { log ->
            val (index, _) = StatsUtils.getBucketIndex(Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate(), range, today)
            energyBurned[index] = (energyBurned[index] ?: 0f) + log.calories.toFloat()
        }

        val weekSessions = allWorkoutLogs.filterWorkoutsIn(TimeRange.Week, today).size
        val monthSessions = allWorkoutLogs.filterWorkoutsIn(TimeRange.Month, today).size
        val yearSessions = allWorkoutLogs.filterWorkoutsIn(TimeRange.Year, today).size

        _energyState.value = EnergyStatsState(
            chartSeries = listOf(
                (1..bucketCount).map { entryOf(it - 1, energyIntake[it] ?: 0f) },
                (1..bucketCount).map { entryOf(it - 1, energyBurned[it] ?: 0f) }
            ),
            workoutSummary = WorkoutSummary(
                sessions = filteredWorkouts.size,
                calories = filteredWorkouts.sumOf { it.calories },
                minutes = filteredWorkouts.sumOf { it.durationMinutes },
                sessionsWeek = weekSessions,
                sessionsMonth = monthSessions,
                sessionsYear = yearSessions
            )
        )

        // 3. Nutrition (Macros)
        val protein = mutableMapOf<Int, Float>()
        val carbs = mutableMapOf<Int, Float>()
        val fat = mutableMapOf<Int, Float>()
        
        allMealLogs.filterMealsIn(range, today).forEach { log ->
            val (index, _) = StatsUtils.getBucketIndex(Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate(), range, today)
            protein[index] = (protein[index] ?: 0f) + log.protein.toFloat()
            carbs[index] = (carbs[index] ?: 0f) + log.carbs.toFloat()
            fat[index] = (fat[index] ?: 0f) + log.fat.toFloat()
        }

        _nutritionState.value = NutritionStatsState(
            chartSeries = listOf(
                (1..bucketCount).map { entryOf(it - 1, protein[it] ?: 0f) },
                (1..bucketCount).map { entryOf(it - 1, carbs[it] ?: 0f) },
                (1..bucketCount).map { entryOf(it - 1, fat[it] ?: 0f) }
            ),
            summaryValue = "${allMealLogs.filterMealsIn(range, today).sumOf { it.calories }} kcal"
        )

        _chartLabels.value = labels
    }

        private fun List<WorkoutLog>.filterWorkoutsIn(range: TimeRange, today: LocalDate) = filter { 

            StatsUtils.getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(), range, today).second 

        }

        

        private fun List<MealLog>.filterMealsIn(range: TimeRange, today: LocalDate) = filter { 

            StatsUtils.getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(), range, today).second 

        }

    }

    