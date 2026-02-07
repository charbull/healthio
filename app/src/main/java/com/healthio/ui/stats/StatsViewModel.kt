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

import com.healthio.core.data.dataStore
import com.healthio.ui.settings.SettingsViewModel

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val fastingRepository = FastingRepository(application)
    private val db = AppDatabase.getDatabase(application)
    private val workoutDao = db.workoutDao()
    private val mealDao = db.mealDao()
    private val weightRepository = com.healthio.core.data.WeightRepository(application)

    private val _timeRange = MutableStateFlow(TimeRange.Week)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    private val _statType = MutableStateFlow(StatType.Intake)
    val statType: StateFlow<StatType> = _statType.asStateFlow()

    private val _chartSeries = MutableStateFlow<List<List<ChartEntry>>>(emptyList())
    val chartSeries: StateFlow<List<List<ChartEntry>>> = _chartSeries.asStateFlow()

    private val _chartLabels = MutableStateFlow<List<String>>(emptyList())
    val chartLabels: StateFlow<List<String>> = _chartLabels.asStateFlow()

    private val _proteinSeries = MutableStateFlow<List<List<ChartEntry>>>(emptyList())
    val proteinSeries: StateFlow<List<List<ChartEntry>>> = _proteinSeries.asStateFlow()

    private val _macroSeries = MutableStateFlow<List<List<ChartEntry>>>(emptyList())
    val macroSeries: StateFlow<List<List<ChartEntry>>> = _macroSeries.asStateFlow()

    private val _weightSeries = MutableStateFlow<List<List<ChartEntry>>>(emptyList())
    val weightSeries: StateFlow<List<List<ChartEntry>>> = _weightSeries.asStateFlow()

    private val _weightUnit = MutableStateFlow("LBS")
    val weightUnit: StateFlow<String> = _weightUnit.asStateFlow()
    
    private val _recentMeals = MutableStateFlow<List<MealLog>>(emptyList())
    val recentMeals: StateFlow<List<MealLog>> = _recentMeals.asStateFlow()
    
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
    private var allWeightLogs: List<com.healthio.core.database.WeightLog> = emptyList()
    private var baseDailyBurn = 1800

    init {
        viewModelScope.launch {
            application.dataStore.data.collect { prefs ->
                baseDailyBurn = prefs[SettingsViewModel.BASE_DAILY_BURN] ?: 1800
                _weightUnit.value = prefs[SettingsViewModel.WEIGHT_UNIT] ?: "LBS"
                updateChartData()
            }
        }
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
        viewModelScope.launch {
            weightRepository.getAllWeights().collect { weights ->
                allWeightLogs = weights
                updateChartData()
            }
        }
        
        // Fetch recent meals (last 30 days)
        val thirtyDaysAgo = LocalDate.now(ZoneId.systemDefault()).minusDays(30)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        viewModelScope.launch {
            mealDao.getMealsSince(thirtyDaysAgo).collect { meals ->
                _recentMeals.value = meals
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

    fun updateMeal(meal: MealLog) {
        viewModelScope.launch {
            mealDao.updateMeal(meal)
        }
    }

    fun deleteMeal(meal: MealLog) {
        viewModelScope.launch {
            mealDao.deleteMeal(meal)
        }
    }

    private fun updateChartData() {
        val range = _timeRange.value
        val type = _statType.value
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        
        val labels = when (range) {
            TimeRange.Week -> {
                // Rolling 7 days: 6 days ago until today
                (0..6).map { offset ->
                    val date = today.minusDays(6L - offset)
                    val name = date.dayOfWeek.name.take(3).lowercase()
                    name.substring(0, 1).uppercase() + name.substring(1)
                }
            }
            TimeRange.Month -> (1..today.lengthOfMonth()).map { it.toString() }
            TimeRange.Year -> listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        }
        val bucketCount = labels.size
        val seriesList = mutableListOf<List<ChartEntry>>()

        _workoutDetails.value = null
        _proteinSeries.value = emptyList()
        _macroSeries.value = emptyList()
        _weightSeries.value = emptyList()

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
                val dailyIntensity = mutableMapOf<Int, Float>()
                // Filter for summary stats
                val filteredWorkouts = allWorkoutLogs.filter { 
                    StatsUtils.getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate(), range, today).second 
                }
                
                // Calculate chart data: Average kcal/min per bucket
                val bucketCalories = mutableMapOf<Int, Float>()
                val bucketMinutes = mutableMapOf<Int, Float>()

                allWorkoutLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, include) = StatsUtils.getBucketIndex(date, range, today)
                    if (include && index in 1..bucketCount) {
                        // Only count actual workouts
                        if (log.type != "Daily Active Burn" && log.type != "Daily BMR & Activity") {
                            bucketCalories[index] = (bucketCalories[index] ?: 0f) + log.calories.toFloat()
                            bucketMinutes[index] = (bucketMinutes[index] ?: 0f) + log.durationMinutes.toFloat()
                        }
                    }
                }
                
                for (i in 1..bucketCount) {
                    val cals = bucketCalories[i] ?: 0f
                    val mins = bucketMinutes[i] ?: 0f
                    if (mins > 0) {
                        dailyIntensity[i] = cals / mins
                    }
                }

                seriesList.add((1..bucketCount).map { entryOf(it - 1, dailyIntensity[it] ?: 0f) })
                
                // Recalculate filtered lists for frequency stats (keep logic for summary card)
                val weekSessions = allWorkoutLogs.count { 
                    it.type != "Daily Active Burn" && it.type != "Daily BMR & Activity" && StatsUtils.getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate(), TimeRange.Week, today).second 
                }
                val monthSessions = allWorkoutLogs.count { 
                    it.type != "Daily Active Burn" && it.type != "Daily BMR & Activity" && StatsUtils.getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate(), TimeRange.Month, today).second 
                }
                val yearSessions = allWorkoutLogs.count { 
                    it.type != "Daily Active Burn" && it.type != "Daily BMR & Activity" && StatsUtils.getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate(), TimeRange.Year, today).second 
                }

                val realSessions = filteredWorkouts.filter { it.type != "Daily Active Burn" && it.type != "Daily BMR & Activity" }
                val totalMins = realSessions.sumOf { it.durationMinutes }
                val avgIntensity = if (totalMins > 0) realSessions.sumOf { it.calories } / totalMins.toFloat() else 0f

                _workoutDetails.value = WorkoutSummary(
                    sessions = realSessions.size,
                    calories = filteredWorkouts.sumOf { it.calories }, // Sum ALL calories including adjustments
                    minutes = totalMins,
                    sessionsWeek = weekSessions,
                    sessionsMonth = monthSessions,
                    sessionsYear = yearSessions
                )
                _summaryTitle.value = "Workout Intensity"
                _summaryValue.value = String.format("%.1f kcal/min", avgIntensity)
            }
            StatType.Intake -> {
                val positiveSeries = mutableMapOf<Int, Float>()
                val negativeSeries = mutableMapOf<Int, Float>()
                val proteinMap = mutableMapOf<Int, Float>()
                val carbsMap = mutableMapOf<Int, Float>()
                val fatMap = mutableMapOf<Int, Float>()
                
                var totalIntake = 0
                var totalBurned = 0
                
                // 1. Intake and Macros
                val intakeMap = mutableMapOf<Int, Int>()
                allMealLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, include) = StatsUtils.getBucketIndex(date, range, today)
                    if (include && index in 1..bucketCount) {
                        intakeMap[index] = (intakeMap[index] ?: 0) + log.calories
                        proteinMap[index] = (proteinMap[index] ?: 0f) + log.protein.toFloat()
                        carbsMap[index] = (carbsMap[index] ?: 0f) + log.carbs.toFloat()
                        fatMap[index] = (fatMap[index] ?: 0f) + log.fat.toFloat()
                        totalIntake += log.calories
                    }
                }
                
                // 2. Burned
                val burnedMap = mutableMapOf<Int, Int>()
                
                // Group workout logs by bucket
                val workoutsByBucket = mutableMapOf<Int, MutableList<WorkoutLog>>()
                allWorkoutLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                    val (index, include) = StatsUtils.getBucketIndex(date, range, today)
                    if (include && index in 1..bucketCount) {
                        workoutsByBucket.getOrPut(index) { mutableListOf() }.add(log)
                    }
                }

                // Calculate total burn per bucket
                for (i in 1..bucketCount) {
                    val logs = workoutsByBucket[i] ?: emptyList()
                    val intake = intakeMap[i] ?: 0
                    
                    val hcDailyLogs = logs.filter { it.type == "Health Connect Daily" }
                    val hcActiveBurnLogs = logs.filter { it.type == "Health Connect Active Burn" || it.type == "Daily Active Burn" }
                    val manualLogs = logs.filter { it.source == "Manual" }
                    val otherIndividualWorkouts = logs.filter { it.source == "Health Connect" && it.type != "Health Connect Daily" && it.type != "Health Connect Active Burn" && it.type != "Daily Active Burn" }

                    // Only count activity if there is a meal log OR a workout log
                    val hasActivity = intake > 0 || manualLogs.isNotEmpty() || otherIndividualWorkouts.isNotEmpty() || hcDailyLogs.isNotEmpty() || hcActiveBurnLogs.isNotEmpty()

                    val bucketBurn = if (!hasActivity) {
                        0 
                    } else if (hcDailyLogs.isNotEmpty()) {
                        // Scenario 1: Health Connect Total Daily Sync (Already contains BMR)
                        hcDailyLogs.sumOf { it.calories } + manualLogs.sumOf { it.calories }
                    } else {
                        // Scenario 2: Active Burn only OR Manual only (Need to add BMR manually)
                        val workoutSum = manualLogs.sumOf { it.calories } + otherIndividualWorkouts.sumOf { it.calories } + hcActiveBurnLogs.sumOf { it.calories }
                        
                        val isFuture = when (range) {
                            TimeRange.Week -> false
                            TimeRange.Month -> i > today.dayOfMonth
                            TimeRange.Year -> i > today.monthValue
                        }
                        
                        val isToday = when (range) {
                            TimeRange.Week -> i == 7
                            TimeRange.Month -> i == today.dayOfMonth
                            TimeRange.Year -> false
                        }
                        
                        var bmrAddition = 0
                        if (!isFuture) {
                            if (range == TimeRange.Year) {
                                if (i == today.monthValue) {
                                    val now = java.time.LocalTime.now(zoneId)
                                    val dayProgress = now.toSecondOfDay() / 86400f
                                    val pastDays = today.dayOfMonth - 1
                                    bmrAddition = (baseDailyBurn * pastDays) + (baseDailyBurn * dayProgress).toInt()
                                } else {
                                    val daysInMonth = java.time.YearMonth.of(today.year, i).lengthOfMonth()
                                    bmrAddition = baseDailyBurn * daysInMonth
                                }
                            } else {
                                if (isToday) {
                                    val now = java.time.LocalTime.now(zoneId)
                                    val dayProgress = now.toSecondOfDay() / 86400f
                                    bmrAddition = (baseDailyBurn * dayProgress).toInt()
                                } else {
                                    bmrAddition = baseDailyBurn
                                }
                            }
                        }
                        workoutSum + bmrAddition
                    }
                    burnedMap[i] = bucketBurn
                    totalBurned += bucketBurn
                }
                
                // 3. Process Series
                val pEntries = mutableListOf<ChartEntry>()
                val cEntries = mutableListOf<ChartEntry>()
                val fEntries = mutableListOf<ChartEntry>()

                for (i in 1..bucketCount) {
                    val intake = intakeMap[i] ?: 0
                    val burned = burnedMap[i] ?: 0
                    
                    val net = (intake - burned).toFloat()
                    
                    if (net > 0) {
                        positiveSeries[i] = net
                        negativeSeries[i] = 0f
                    } else {
                        positiveSeries[i] = 0f
                        negativeSeries[i] = net
                    }

                    // Macro Percentages
                    val p = proteinMap[i] ?: 0f
                    val c = carbsMap[i] ?: 0f
                    val f = fatMap[i] ?: 0f
                    val totalMacros = p + c + f
                    if (totalMacros > 0) {
                        pEntries.add(MacroEntry(i - 1f, (p / totalMacros) * 100f, p.toInt(), "P"))
                        cEntries.add(MacroEntry(i - 1f, (c / totalMacros) * 100f, c.toInt(), "C"))
                        fEntries.add(MacroEntry(i - 1f, (f / totalMacros) * 100f, f.toInt(), "F"))
                    } else {
                        pEntries.add(MacroEntry(i - 1f, 0f, 0, "P"))
                        cEntries.add(MacroEntry(i - 1f, 0f, 0, "C"))
                        fEntries.add(MacroEntry(i - 1f, 0f, 0, "F"))
                    }
                }
                
                seriesList.add((1..bucketCount).map { entryOf(it - 1, positiveSeries[it] ?: 0f) })
                seriesList.add((1..bucketCount).map { entryOf(it - 1, negativeSeries[it] ?: 0f) })
                
                // 4. Weight Series
                val weightEntries = WeightSeriesCalculator.calculate(
                    allWeightLogs = allWeightLogs,
                    range = range,
                    today = today,
                    zoneId = zoneId,
                    unit = _weightUnit.value
                )
                _weightSeries.value = listOf(weightEntries)
                
                _macroSeries.value = listOf(pEntries, cEntries, fEntries)
                _summaryTitle.value = ""
                _summaryValue.value = ""
            }
        }

        _chartSeries.value = seriesList
        _chartLabels.value = labels
    }
}
