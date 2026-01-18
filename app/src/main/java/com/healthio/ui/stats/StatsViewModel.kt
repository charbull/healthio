package com.healthio.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthio.core.data.FastingRepository
import com.healthio.core.database.FastingLog
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

enum class TimeRange {
    Week, Month, Year
}

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FastingRepository(application)

    private val _timeRange = MutableStateFlow(TimeRange.Week)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    private val _chartEntries = MutableStateFlow<List<ChartEntry>>(emptyList())
    val chartEntries: StateFlow<List<ChartEntry>> = _chartEntries.asStateFlow()

    private val _chartLabels = MutableStateFlow<List<String>>(emptyList())
    val chartLabels: StateFlow<List<String>> = _chartLabels.asStateFlow()

    // Keep raw logs to re-aggregate
    private var allLogs: List<FastingLog> = emptyList()

    init {
        viewModelScope.launch {
            repository.fastingHistory.collect { history ->
                allLogs = history
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

        when (range) {
            TimeRange.Week -> {
                // Last 7 days (or Mon-Sun)
                // Let's do Mon-Sun relative to current week or last 7 days?
                // The previous impl was static "Mon".."Sun". Let's stick to that for consistency.
                labels.addAll(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
                
                // Aggregation
                val dailyMax = mutableMapOf<Int, Float>() // 1..7 -> Hours
                
                allLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.endTime).atZone(zoneId).toLocalDate()
                    // Filter for this week? Or just last 7 days?
                    // Simple: Last 7 days.
                    val daysDiff = ChronoUnit.DAYS.between(date, today)
                    if (daysDiff in 0..6) {
                        val dayOfWeek = date.dayOfWeek.value // 1..7
                        val hours = (log.durationMillis / (1000f * 60 * 60)).coerceAtLeast(0f)
                        val currentMax = dailyMax[dayOfWeek] ?: 0f
                        if (hours > currentMax) dailyMax[dayOfWeek] = hours
                    }
                }
                
                for (i in 1..7) {
                    entries.add(entryOf(i - 1, dailyMax[i] ?: 0f))
                }
            }
            TimeRange.Month -> {
                // Current Month (1..30/31)
                val yearMonth = YearMonth.now(zoneId)
                val daysInMonth = yearMonth.lengthOfMonth()
                
                // Labels: 1, 5, 10, 15, 20, 25, 30? Or just indices. 
                // Vico handles dense labels well usually, but 30 strings might overlap.
                // We'll pass all strings, Vico can skip if we configure axis (or we pass empty strings).
                // Let's pass numbers.
                for (d in 1..daysInMonth) labels.add(d.toString())
                
                val dailyMax = mutableMapOf<Int, Float>()
                
                allLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.endTime).atZone(zoneId).toLocalDate()
                    if (YearMonth.from(date) == yearMonth) {
                        val day = date.dayOfMonth
                        val hours = (log.durationMillis / (1000f * 60 * 60)).coerceAtLeast(0f)
                        val currentMax = dailyMax[day] ?: 0f
                        if (hours > currentMax) dailyMax[day] = hours
                    }
                }
                
                for (i in 1..daysInMonth) {
                    entries.add(entryOf(i - 1, dailyMax[i] ?: 0f))
                }
            }
            TimeRange.Year -> {
                // Jan..Dec
                labels.addAll(listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"))
                
                val monthlyTotal = mutableMapOf<Int, Float>()
                val monthlyCount = mutableMapOf<Int, Int>()
                
                val currentYear = today.year
                
                allLogs.forEach { log ->
                    val date = Instant.ofEpochMilli(log.endTime).atZone(zoneId).toLocalDate()
                    if (date.year == currentYear) {
                        val month = date.monthValue // 1..12
                        val hours = (log.durationMillis / (1000f * 60 * 60)).coerceAtLeast(0f)
                        monthlyTotal[month] = (monthlyTotal[month] ?: 0f) + hours
                        monthlyCount[month] = (monthlyCount[month] ?: 0) + 1
                    }
                }
                
                for (i in 1..12) {
                    // Average
                    val total = monthlyTotal[i] ?: 0f
                    val count = monthlyCount[i] ?: 1
                    val avg = if (count > 0) total / count else 0f
                    entries.add(entryOf(i - 1, avg))
                }
            }
        }
        
        _chartEntries.value = entries
        _chartLabels.value = labels
    }
}