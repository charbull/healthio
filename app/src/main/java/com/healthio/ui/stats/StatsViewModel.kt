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

    private val _statType = MutableStateFlow(StatType.Fasting)
    val statType: StateFlow<StatType> = _statType.asStateFlow()

    private val _chartSeries = MutableStateFlow<List<List<ChartEntry>>>(emptyList())
    val chartSeries: StateFlow<List<List<ChartEntry>>> = _chartSeries.asStateFlow()

    private val _chartLabels = MutableStateFlow<List<String>>(emptyList())
    val chartLabels: StateFlow<List<String>> = _chartLabels.asStateFlow()
    
    // Detailed summary data for the bottom card
    private val _summaryTitle = MutableStateFlow("")
    val summaryTitle: StateFlow<String> = _summaryTitle.asStateFlow()

    private val _summaryValue = MutableStateFlow("")
    val summaryValue: StateFlow<String> = _summaryValue.asStateFlow()
    
    private val _workoutDetails = MutableStateFlow<WorkoutSummary?>(null)
    val workoutDetails: StateFlow<WorkoutSummary?> = _workoutDetails.asStateFlow()

    data class WorkoutSummary(val sessions: Int, val calories: Int, val minutes: Int)

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

    fun setStatType(type: StatType) {
        _statType.value = type
        updateChartData()
    }

    private fun updateChartData() {
        val range = _timeRange.value
        val type = _statType.value
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        
        val labels = when (range) {
            TimeRange.Week -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            TimeRange.Month -> (1..YearMonth.now(zoneId).lengthOfMonth()).map { it.toString() }
            TimeRange.Year -> listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        }
        val bucketCount = labels.size
        val seriesList = mutableListOf<List<ChartEntry>>()
        val dailyValues = mutableMapOf<Int, Float>()

        // Reset details
        _workoutDetails.value = null

        when (type) {
            StatType.Fasting -> {
                allFastingLogs.forEach { log ->
                    var currentStart = Instant.ofEpochMilli(log.startTime).atZone(zoneId)
                    val endDateTime = Instant.ofEpochMilli(log.endTime).atZone(zoneId)
                    while (currentStart.isBefore(endDateTime)) {
                        val endOfDay = currentStart.toLocalDate().plusDays(1).atStartOfDay(zoneId)
                        val segmentEnd = if (endDateTime.isBefore(endOfDay)) endDateTime else endOfDay
                        val (index, include) = getBucketIndex(currentStart.toLocalDate(), range, today)
                        if (include) {
                            val durationHrs = ChronoUnit.MILLIS.between(currentStart, segmentEnd) / 3600000f
                            dailyValues[index] = maxOf(dailyValues[index] ?: 0f, durationHrs)
                        }
                        currentStart = segmentEnd
                    }
                }
                seriesList.add((1..bucketCount).map { entryOf(it - 1, dailyValues[it] ?: 0f) })
                _summaryTitle.value = "Fasting"
                _summaryValue.value = "Tracking consistency"
            }
            StatType.Workouts -> {
                val currentWorkouts = allWorkoutLogs.filterWorkoutsIn(range, today)
                currentWorkouts.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, _) = getBucketIndex(date, range, today)
                    dailyValues[index] = (dailyValues[index] ?: 0f) + 1f
                }
                seriesList.add((1..bucketCount).map { entryOf(it - 1, dailyValues[it] ?: 0f) })
                
                _workoutDetails.value = WorkoutSummary(
                    sessions = currentWorkouts.size,
                    calories = currentWorkouts.sumOf { it.calories },
                    minutes = currentWorkouts.sumOf { it.durationMinutes }
                )
                _summaryTitle.value = "Workout Activity"
                _summaryValue.value = "${currentWorkouts.size} sessions"
            }
            StatType.Calories -> {
                val currentMeals = allMealLogs.filterMealsIn(range, today)
                currentMeals.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, _) = getBucketIndex(date, range, today)
                    dailyValues[index] = (dailyValues[index] ?: 0f) + log.calories.toFloat()
                }
                seriesList.add((1..bucketCount).map { entryOf(it - 1, dailyValues[it] ?: 0f) })
                _summaryTitle.value = "Total Intake"
                _summaryValue.value = "${currentMeals.sumOf { it.calories }} kcal"
            }
            StatType.Macros -> {
                val protein = mutableMapOf<Int, Float>()
                val carbs = mutableMapOf<Int, Float>()
                val fat = mutableMapOf<Int, Float>()
                val currentMeals = allMealLogs.filterMealsIn(range, today)
                currentMeals.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, _) = getBucketIndex(date, range, today)
                    protein[index] = (protein[index] ?: 0f) + log.protein.toFloat()
                    carbs[index] = (carbs[index] ?: 0f) + log.carbs.toFloat()
                    fat[index] = (fat[index] ?: 0f) + log.fat.toFloat()
                }
                seriesList.add((1..bucketCount).map { entryOf(it - 1, protein[it] ?: 0f) })
                seriesList.add((1..bucketCount).map { entryOf(it - 1, carbs[it] ?: 0f) })
                seriesList.add((1..bucketCount).map { entryOf(it - 1, fat[it] ?: 0f) })
                _summaryTitle.value = "Nutrition"
                _summaryValue.value = "${currentMeals.sumOf { it.calories }} kcal intake"
            }
        }

        _chartSeries.value = seriesList
        _chartLabels.value = labels
    }

    private fun List<WorkoutLog>.filterWorkoutsIn(range: TimeRange, today: LocalDate) = filter { 
        getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(), range, today).second 
    }
    
    private fun List<MealLog>.filterMealsIn(range: TimeRange, today: LocalDate) = filter { 
        getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(), range, today).second 
    }

    private fun getBucketIndex(date: LocalDate, range: TimeRange, today: LocalDate): Pair<Int, Boolean> {
        return when (range) {
            TimeRange.Week -> {
                val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                val endOfWeek = startOfWeek.plusDays(6)
                val isThisWeek = !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
                Pair(date.dayOfWeek.value, isThisWeek)
            }
            TimeRange.Month -> {
                Pair(date.dayOfMonth, YearMonth.from(date) == YearMonth.from(today))
            }
            TimeRange.Year -> {
                Pair(date.monthValue, date.year == today.year)
            }
        }
    }
}
