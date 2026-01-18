package com.healthio.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthio.core.data.FastingRepository
import com.healthio.core.data.WorkoutRepository
import com.healthio.core.database.AppDatabase
import com.healthio.core.database.FastingLog
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
enum class StatType { Fasting, Workouts }

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val fastingRepository = FastingRepository(application)
    private val workoutDao = AppDatabase.getDatabase(application).workoutDao()

    private val _timeRange = MutableStateFlow(TimeRange.Week)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    private val _statType = MutableStateFlow(StatType.Fasting)
    val statType: StateFlow<StatType> = _statType.asStateFlow()

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
        
        val entries = mutableListOf<ChartEntry>()
        val labels = mutableListOf<String>()
        var count = 0

        // Determine range parameters
        val labelList = when (range) {
            TimeRange.Week -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            TimeRange.Month -> (1..YearMonth.now(zoneId).lengthOfMonth()).map { it.toString() }
            TimeRange.Year -> listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        }
        labels.addAll(labelList)

        val bucketCount = labelList.size
        val dailyValues = mutableMapOf<Int, Float>()

        if (type == StatType.Fasting) {
            // Existing Fasting Logic
            allFastingLogs.forEach { log ->
                val date = Instant.ofEpochMilli(log.endTime).atZone(zoneId).toLocalDate()
                val (index, include) = getBucketIndex(date, range, today)
                if (include) {
                    val hours = (log.durationMillis / (1000f * 60 * 60)).coerceAtLeast(0f)
                    dailyValues[index] = maxOf(dailyValues[index] ?: 0f, hours)
                }
            }
        } else {
            // Workout Count Logic
            allWorkoutLogs.forEach { log ->
                val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                val (index, include) = getBucketIndex(date, range, today)
                if (include) {
                    dailyValues[index] = (dailyValues[index] ?: 0f) + 1f
                }
            }
        }

        for (i in 0 until bucketCount) {
            entries.add(entryOf(i, dailyValues[i+1] ?: 0f))
        }

        // Global count for the card
        count = allWorkoutLogs.count { log ->
            val date = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
            val (_, include) = getBucketIndex(date, range, today)
            include
        }

        _chartEntries.value = entries
        _chartLabels.value = labels
        _workoutCount.value = count
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