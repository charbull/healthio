package com.healthio.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthio.core.data.FastingRepository
import com.healthio.core.data.WorkoutRepository
import com.healthio.core.database.FastingLog
import com.healthio.core.database.WorkoutLog
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class TimeRange {
    Week, Month, Year
}

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val fastingRepository = FastingRepository(application)
    private val workoutRepository = WorkoutRepository(application) // Note: Need to add workoutHistory to Repo

    private val _timeRange = MutableStateFlow(TimeRange.Week)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    private val _chartEntries = MutableStateFlow<List<ChartEntry>>(emptyList())
    val chartEntries: StateFlow<List<ChartEntry>> = _chartEntries.asStateFlow()

    private val _chartLabels = MutableStateFlow<List<String>>(emptyList())
    val chartLabels: StateFlow<List<String>> = _chartLabels.asStateFlow()
    
    private val _workoutCount = MutableStateFlow(0)
    val workoutCount: StateFlow<Int> = _workoutCount.asStateFlow()

    private var allFastingLogs: List<FastingLog> = emptyList()
    private var allWorkoutLogs: List<WorkoutLog> = emptyList()

    init {
        viewModelScope.launch {
            // I'll need to update WorkoutRepository to expose allWorkouts Flow
            // For now, I'll use fasting logs and assume workout data is coming soon
            fastingRepository.fastingHistory.collect { history ->
                allFastingLogs = history
                updateChartData(_timeRange.value)
            }
        }
        
        viewModelScope.launch {
            // Mocking workout collection until Repo is updated
            com.healthio.core.database.AppDatabase.getDatabase(application).workoutDao().getAllWorkouts().collect { workouts ->
                allWorkoutLogs = workouts
                updateChartData(_timeRange.value)
            }
        }
    }

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
        updateChartData(range)
    }

    private fun updateChartData(range: TimeRange) {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val entries = mutableListOf<ChartEntry>()
        val labels = mutableListOf<String>()
        var count = 0

        when (range) {
            TimeRange.Week -> {
                labels.addAll(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
                val dailyMax = mutableMapOf<Int, Float>()
                allFastingLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.endTime).atZone(zoneId).toLocalDate()
                    val daysDiff = ChronoUnit.DAYS.between(date, today)
                    if (daysDiff in 0..6) {
                        val dayOfWeek = date.dayOfWeek.value
                        val hours = (log.durationMillis / (1000f * 60 * 60)).coerceAtLeast(0f)
                        val currentMax = dailyMax[dayOfWeek] ?: 0f
                        if (hours > currentMax) dailyMax[dayOfWeek] = hours
                    }
                }
                for (i in 1..7) entries.add(entryOf(i - 1, dailyMax[i] ?: 0f))
                
                count = allWorkoutLogs.count { 
                    val date = Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
                    ChronoUnit.DAYS.between(date, today) in 0..6
                }
            }
            TimeRange.Month -> {
                val yearMonth = YearMonth.now(zoneId)
                val daysInMonth = yearMonth.lengthOfMonth()
                for (d in 1..daysInMonth) labels.add(d.toString())
                val dailyMax = mutableMapOf<Int, Float>()
                allFastingLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.endTime).atZone(zoneId).toLocalDate()
                    if (YearMonth.from(date) == yearMonth) {
                        val day = date.dayOfMonth
                        val hours = (log.durationMillis / (1000f * 60 * 60)).coerceAtLeast(0f)
                        val currentMax = dailyMax[day] ?: 0f
                        if (hours > currentMax) dailyMax[day] = hours
                    }
                }
                for (i in 1..daysInMonth) entries.add(entryOf(i - 1, dailyMax[i] ?: 0f))
                
                count = allWorkoutLogs.count { 
                    val date = Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
                    YearMonth.from(date) == yearMonth
                }
            }
            TimeRange.Year -> {
                labels.addAll(listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"))
                val monthlyTotal = mutableMapOf<Int, Float>()
                val monthlyCount = mutableMapOf<Int, Int>()
                val currentYear = today.year
                allFastingLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.endTime).atZone(zoneId).toLocalDate()
                    if (date.year == currentYear) {
                        val month = date.monthValue
                        val hours = (log.durationMillis / (1000f * 60 * 60)).coerceAtLeast(0f)
                        monthlyTotal[month] = (monthlyTotal[month] ?: 0f) + hours
                        monthlyCount[month] = (monthlyCount[month] ?: 0) + 1
                    }
                }
                for (i in 1..12) {
                    val total = monthlyTotal[i] ?: 0f
                    val c = monthlyCount[i] ?: 1
                    val avg = if (c > 0) total / c else 0f
                    entries.add(entryOf(i - 1, avg))
                }
                
                count = allWorkoutLogs.count { 
                    val date = Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate()
                    date.year == currentYear
                }
            }
        }
        
        _chartEntries.value = entries
        _chartLabels.value = labels
        _workoutCount.value = count
    }
}
