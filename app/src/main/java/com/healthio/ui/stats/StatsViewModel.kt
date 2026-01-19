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
    
    private val _summaryTitle = MutableStateFlow("")
    val summaryTitle: StateFlow<String> = _summaryTitle.asStateFlow()

    private val _summaryValue = MutableStateFlow("")
    val summaryValue: StateFlow<String> = _summaryValue.asStateFlow()
    
    private val _workoutDetails = MutableStateFlow<WorkoutSummary?>(null)
    val workoutDetails: StateFlow<WorkoutSummary?> = _workoutDetails.asStateFlow()

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

        _workoutDetails.value = null

        when (type) {
            StatType.Fasting -> {
                val dailyTotal = mutableMapOf<Int, Float>()
                allFastingLogs.forEach { log ->
                    var currentStart = Instant.ofEpochMilli(log.startTime).atZone(zoneId)
                    val endDateTime = Instant.ofEpochMilli(log.endTime).atZone(zoneId)
                    while (currentStart.isBefore(endDateTime)) {
                        val endOfDay = currentStart.toLocalDate().plusDays(1).atStartOfDay(zoneId)
                        val segmentEnd = if (endDateTime.isBefore(endOfDay)) endDateTime else endOfDay
                        val (index, include) = getBucketIndex(currentStart.toLocalDate(), range, today)
                        if (include) {
                            val durationHrs = ChronoUnit.MILLIS.between(currentStart, segmentEnd) / 3600000f
                            dailyTotal[index] = (dailyTotal[index] ?: 0f) + durationHrs
                        }
                        currentStart = segmentEnd
                    }
                }
                seriesList.add((1..bucketCount).map { entryOf(it - 1, dailyTotal[it] ?: 0f) })
                _summaryTitle.value = "Fasting"
                _summaryValue.value = "Active consistency"
            }
            StatType.Workouts -> {
                val dailyCounts = mutableMapOf<Int, Float>()
                val filtered = allWorkoutLogs.filterWorkoutsIn(range, today)
                filtered.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, include) = getBucketIndex(date, range, today)
                    if (include) {
                        dailyCounts[index] = (dailyCounts[index] ?: 0f) + 1f
                    }
                }
                seriesList.add((1..bucketCount).map { entryOf(it - 1, dailyCounts[it] ?: 0f) })
                
                val weekSessions = allWorkoutLogs.filterWorkoutsIn(TimeRange.Week, today).size
                val monthSessions = allWorkoutLogs.filterWorkoutsIn(TimeRange.Month, today).size
                val yearSessions = allWorkoutLogs.filterWorkoutsIn(TimeRange.Year, today).size

                _workoutDetails.value = WorkoutSummary(
                    sessions = filtered.size,
                    calories = filtered.sumOf { it.calories },
                    minutes = filtered.sumOf { it.durationMinutes },
                    sessionsWeek = weekSessions,
                    sessionsMonth = monthSessions,
                    sessionsYear = yearSessions
                )
                _summaryTitle.value = "Workout Activity"
                _summaryValue.value = "${filtered.size} sessions"
            }
            StatType.Calories -> {
                val dailyTotals = mutableMapOf<Int, Float>()
                val filtered = allMealLogs.filterMealsIn(range, today)
                filtered.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, include) = getBucketIndex(date, range, today)
                    if (include) {
                        dailyTotals[index] = (dailyTotals[index] ?: 0f) + log.calories.toFloat()
                    }
                }
                seriesList.add((1..bucketCount).map { entryOf(it - 1, dailyTotals[it] ?: 0f) })
                _summaryTitle.value = "Energy Intake"
                _summaryValue.value = "${filtered.sumOf { it.calories }} kcal"
            }
            StatType.Macros -> {
                val protein = mutableMapOf<Int, Float>()
                val carbs = mutableMapOf<Int, Float>()
                val fat = mutableMapOf<Int, Float>()
                val filtered = allMealLogs.filterMealsIn(range, today)
                filtered.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, include) = getBucketIndex(date, range, today)
                    if (include) {
                        protein[index] = (protein[index] ?: 0f) + log.protein.toFloat()
                        carbs[index] = (carbs[index] ?: 0f) + log.carbs.toFloat()
                        fat[index] = (fat[index] ?: 0f) + log.fat.toFloat()
                    }
                }
                seriesList.add((1..bucketCount).map { entryOf(it - 1, protein[it] ?: 0f) })
                seriesList.add((1..bucketCount).map { entryOf(it - 1, carbs[it] ?: 0f) })
                seriesList.add((1..bucketCount).map { entryOf(it - 1, fat[it] ?: 0f) })
                _summaryTitle.value = "Nutrition"
                _summaryValue.value = "${filtered.sumOf { it.calories }} kcal intake"
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
                // Determine the Mon-Sun of the current week
                val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                val endOfWeek = startOfWeek.plusDays(6)
                val isThisWeek = !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
                Pair(date.dayOfWeek.value, isThisWeek)
            }
            TimeRange.Month -> {
                Pair(date.dayOfMonth, date.year == today.year && date.monthValue == today.monthValue)
            }
            TimeRange.Year -> {
                Pair(date.monthValue, date.year == today.year)
            }
        }
    }
}