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

    private val _proteinSeries = MutableStateFlow<List<List<ChartEntry>>>(emptyList())
    val proteinSeries: StateFlow<List<List<ChartEntry>>> = _proteinSeries.asStateFlow()
    
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
            TimeRange.Month -> (1..today.lengthOfMonth()).map { it.toString() }
            TimeRange.Year -> listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        }
        val bucketCount = labels.size
        val seriesList = mutableListOf<List<ChartEntry>>()

        _workoutDetails.value = null
        _proteinSeries.value = emptyList()

        when (type) {
            StatType.Fasting -> {
                val dailyTotal = mutableMapOf<Int, Float>()
                allFastingLogs.forEach { log ->
                    var currentStart = Instant.ofEpochMilli(log.startTime).atZone(zoneId)
                    val endDateTime = Instant.ofEpochMilli(log.endTime).atZone(zoneId)
                    while (currentStart.isBefore(endDateTime)) {
                        val endOfDay = currentStart.toLocalDate().plusDays(1).atStartOfDay(zoneId)
                        val segmentEnd = if (endDateTime.isBefore(endOfDay)) endDateTime else endOfDay
                        val (index, include) = StatsUtils.getBucketIndex(currentStart.toLocalDate(), range, today)
                        if (include && index in 1..bucketCount) {
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
                // Filter for summary stats
                val filteredWorkouts = allWorkoutLogs.filter { 
                    StatsUtils.getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate(), range, today).second 
                }
                
                allWorkoutLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, include) = StatsUtils.getBucketIndex(date, range, today)
                    if (include && index in 1..bucketCount) {
                        // Only count actual workouts for the chart frequency, ignore daily adjustments
                        if (log.type != "Daily Active Burn") {
                            dailyCounts[index] = (dailyCounts[index] ?: 0f) + 1f
                        }
                    }
                }
                seriesList.add((1..bucketCount).map { entryOf(it - 1, dailyCounts[it] ?: 0f) })
                
                // Recalculate filtered lists for frequency stats
                val weekSessions = allWorkoutLogs.count { 
                    it.type != "Daily Active Burn" && StatsUtils.getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate(), TimeRange.Week, today).second 
                }
                val monthSessions = allWorkoutLogs.count { 
                    it.type != "Daily Active Burn" && StatsUtils.getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate(), TimeRange.Month, today).second 
                }
                val yearSessions = allWorkoutLogs.count { 
                    it.type != "Daily Active Burn" && StatsUtils.getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate(), TimeRange.Year, today).second 
                }

                val realSessions = filteredWorkouts.filter { it.type != "Daily Active Burn" }

                _workoutDetails.value = WorkoutSummary(
                    sessions = realSessions.size,
                    calories = filteredWorkouts.sumOf { it.calories }, // Sum ALL calories including adjustments
                    minutes = realSessions.sumOf { it.durationMinutes },
                    sessionsWeek = weekSessions,
                    sessionsMonth = monthSessions,
                    sessionsYear = yearSessions
                )
                _summaryTitle.value = "Workout Activity"
                _summaryValue.value = "${realSessions.size} sessions"
            }
            StatType.Calories -> {
                val dailyTotals = mutableMapOf<Int, Float>()
                var totalCalories = 0
                
                allMealLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, include) = StatsUtils.getBucketIndex(date, range, today)
                    if (include) {
                        if (index in 1..bucketCount) {
                            dailyTotals[index] = (dailyTotals[index] ?: 0f) + log.calories.toFloat()
                        }
                        totalCalories += log.calories
                    }
                }
                seriesList.add((1..bucketCount).map { entryOf(it - 1, dailyTotals[it] ?: 0f) })
                _summaryTitle.value = "Energy Intake"
                _summaryValue.value = "$totalCalories kcal"
            }
            StatType.Macros -> {
                val protein = mutableMapOf<Int, Float>()
                val carbs = mutableMapOf<Int, Float>()
                val fat = mutableMapOf<Int, Float>()
                var totalCalories = 0
                var totalProtein = 0
                
                allMealLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, include) = StatsUtils.getBucketIndex(date, range, today)
                    if (include) {
                        if (index in 1..bucketCount) {
                            protein[index] = (protein[index] ?: 0f) + log.protein.toFloat()
                            carbs[index] = (carbs[index] ?: 0f) + log.carbs.toFloat()
                            fat[index] = (fat[index] ?: 0f) + log.fat.toFloat()
                        }
                        totalCalories += log.calories
                        totalProtein += log.protein
                    }
                }

                val pEntries = mutableListOf<ChartEntry>()
                val cEntries = mutableListOf<ChartEntry>()
                val fEntries = mutableListOf<ChartEntry>()
                val macroLabels = mutableListOf<String>()

                for (i in 1..bucketCount) {
                    val p = protein[i] ?: 0f
                    val c = carbs[i] ?: 0f
                    val f = fat[i] ?: 0f
                    val total = p + c + f
                    
                    val baseLabel = labels[i-1]
                    if (range == TimeRange.Week && p > 0) {
                        macroLabels.add("$baseLabel\n${p.toInt()}g")
                    } else {
                        macroLabels.add(baseLabel)
                    }

                    if (total > 0) {
                        pEntries.add(MacroEntry(i - 1f, (p / total) * 100f, p.toInt(), "P"))
                        cEntries.add(MacroEntry(i - 1f, (c / total) * 100f, c.toInt(), "C"))
                        fEntries.add(MacroEntry(i - 1f, (f / total) * 100f, f.toInt(), "F"))
                    } else {
                        pEntries.add(MacroEntry(i - 1f, 0f, 0, "P"))
                        cEntries.add(MacroEntry(i - 1f, 0f, 0, "C"))
                        fEntries.add(MacroEntry(i - 1f, 0f, 0, "F"))
                    }
                }

                seriesList.add(pEntries)
                seriesList.add(cEntries)
                seriesList.add(fEntries)
                
                // Populate Protein Series for secondary chart
                val proteinTotals = (1..bucketCount).map { entryOf(it - 1, protein[it] ?: 0f) }
                _proteinSeries.value = listOf(proteinTotals)

                _chartLabels.value = macroLabels
                _summaryTitle.value = "Nutrition"
                _summaryValue.value = "$totalProtein g Protein ($totalCalories kcal)"
            }
            StatType.Protein -> {
                 // Removed - merged into Macros
            }
        }

        _chartSeries.value = seriesList
        if (type != StatType.Macros) {
            _chartLabels.value = labels
        }
    }
}

    