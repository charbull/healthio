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

enum class TimeRange { Week, Month, Year }
enum class StatType { Fasting, Workouts, Calories, Macros }

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
    
    private val _workoutCount = MutableStateFlow(0)
    val workoutCount: StateFlow<Int> = _workoutCount.asStateFlow()

    private val _totalCalories = MutableStateFlow(0)
    val totalCalories: StateFlow<Int> = _totalCalories.asStateFlow()

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
        
        val labels = mutableListOf<String>()
        val labelList = when (range) {
            TimeRange.Week -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            TimeRange.Month -> (1..YearMonth.now(zoneId).lengthOfMonth()).map { it.toString() }
            TimeRange.Year -> listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        }
        labels.addAll(labelList)
        val bucketCount = labelList.size

        val seriesList = mutableListOf<List<ChartEntry>>()

        when (type) {
            StatType.Fasting -> {
                val dailyValues = mutableMapOf<Int, Float>()
                allFastingLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.endTime).atZone(zoneId).toLocalDate()
                    val (index, include) = getBucketIndex(date, range, today)
                    if (include) {
                        val hours = (log.durationMillis / (1000f * 60 * 60)).coerceAtLeast(0f)
                        dailyValues[index] = maxOf(dailyValues[index] ?: 0f, hours)
                    }
                }
                seriesList.add((0 until bucketCount).map { entryOf(it, dailyValues[it+1] ?: 0f) })
            }
            StatType.Workouts -> {
                val dailyValues = mutableMapOf<Int, Float>()
                allWorkoutLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, include) = getBucketIndex(date, range, today)
                    if (include) dailyValues[index] = (dailyValues[index] ?: 0f) + 1f
                }
                seriesList.add((0 until bucketCount).map { entryOf(it, dailyValues[it+1] ?: 0f) })
            }
            StatType.Calories -> {
                val dailyValues = mutableMapOf<Int, Float>()
                allMealLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, include) = getBucketIndex(date, range, today)
                    if (include) dailyValues[index] = (dailyValues[index] ?: 0f) + log.calories.toFloat()
                }
                seriesList.add((0 until bucketCount).map { entryOf(it, dailyValues[it+1] ?: 0f) })
            }
            StatType.Macros -> {
                val protein = mutableMapOf<Int, Float>()
                val carbs = mutableMapOf<Int, Float>()
                val fat = mutableMapOf<Int, Float>()
                allMealLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, include) = getBucketIndex(date, range, today)
                    if (include) {
                        protein[index] = (protein[index] ?: 0f) + log.protein.toFloat()
                        carbs[index] = (carbs[index] ?: 0f) + log.carbs.toFloat()
                        fat[index] = (fat[index] ?: 0f) + log.fat.toFloat()
                    }
                }
                seriesList.add((0 until bucketCount).map { entryOf(it, protein[it+1] ?: 0f) })
                seriesList.add((0 until bucketCount).map { entryOf(it, carbs[it+1] ?: 0f) })
                seriesList.add((0 until bucketCount).map { entryOf(it, fat[it+1] ?: 0f) })
            }
        }

        // Aggregate totals for info cards
        _workoutCount.value = allWorkoutLogs.count { log ->
            val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
            val (_, include) = getBucketIndex(date, range, today)
            include
        }
        
        _totalCalories.value = allMealLogs.filter { log ->
            val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
            val (_, include) = getBucketIndex(date, range, today)
            include
        }.sumOf { it.calories }

        _chartSeries.value = seriesList
        _chartLabels.value = labels
    }

    private fun getBucketIndex(date: LocalDate, range: TimeRange, today: LocalDate): Pair<Int, Boolean> {
        return when (range) {
            TimeRange.Week -> {
                val diff = ChronoUnit.DAYS.between(date, today)
                Pair(date.dayOfWeek.value, diff in 0..6)
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