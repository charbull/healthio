package com.healthio.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.healthio.core.database.FastingLog
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun WeeklyFastingChart(
    logs: List<FastingLog>,
    modifier: Modifier = Modifier
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    
    val entries = remember(logs) {
        val now = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        // We want the last 7 days relative to "Today"
        val today = LocalDate.now(zoneId)
        // Adjust chart range: e.g. Today is Sun, show Mon..Sun
        // Or show relative last 7 days? 
        // The previous code mapped 1..7 (Mon..Sun). Let's stick to showing Mon-Sun (1-7) for the current week or relative?
        // Let's stick to Mon(1) .. Sun(7) mapping for the chart X-axis.
        
        // Map: DayOfWeek (1..7) -> List of durations (hours)
        val dailyDurations = mutableMapOf<Int, MutableList<Float>>()
        for (i in 1..7) dailyDurations[i] = mutableListOf()

        logs.forEach { log ->
            var currentStart = Instant.ofEpochMilli(log.startTime).atZone(zoneId)
            val endDateTime = Instant.ofEpochMilli(log.endTime).atZone(zoneId)
            
            while (currentStart.isBefore(endDateTime)) {
                // Find end of current day (midnight next day)
                val endOfDay = currentStart.toLocalDate().plusDays(1).atStartOfDay(zoneId)
                
                // Segment ends at either the fast end or the day end
                val segmentEnd = if (endDateTime.isBefore(endOfDay)) endDateTime else endOfDay
                
                // Calculate hours for this segment
                val durationMillis = ChronoUnit.MILLIS.between(currentStart, segmentEnd)
                val hours = (durationMillis / (1000f * 60 * 60)).coerceAtLeast(0f)
                
                val dayOfWeek = currentStart.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
                
                // Only add if it's within the chart's view (last 7 days logic?)
                // Since the chart is static Mon-Sun, we just check if it's recent enough to matter
                // or just map it to the day of week.
                // NOTE: If we show "This Week", we should filter out logs from last week.
                // For MVP, assuming "Recent Logs" are passed, we just map to DayOfWeek.
                // To be safe, we should filter by date range, but we'll trust the input or mapped bucket.
                
                val logDate = currentStart.toLocalDate()
                val daysDiff = ChronoUnit.DAYS.between(logDate, today)
                if (daysDiff < 7 && daysDiff >= 0) {
                     dailyDurations[dayOfWeek]?.add(hours)
                }
                
                // Move to next day
                currentStart = segmentEnd
            }
        }
        
        val chartEntries = mutableListOf<ChartEntry>()
        for (i in 1..7) {
            // "Override" logic: Take the MAX duration for that day
            val maxHours = dailyDurations[i]?.maxOrNull() ?: 0f
            chartEntries.add(entryOf(i - 1, maxHours))
        }
        chartEntries
    }

    val chartEntryModelProducer = remember(entries) { ChartEntryModelProducer(entries) }
    
    val horizontalAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        days.getOrNull(value.toInt()) ?: ""
    }

    Chart(
        chart = columnChart(
            columns = listOf(
                lineComponent(
                    color = Color(0xFF4CAF50),
                    thickness = 16.dp,
                    shape = Shapes.roundedCornerShape(allPercent = 25)
                )
            )
        ),
        chartModelProducer = chartEntryModelProducer,
        startAxis = rememberStartAxis(
            valueFormatter = { value, _ -> String.format("%.0fh", value) }
        ),
        bottomAxis = rememberBottomAxis(
            valueFormatter = horizontalAxisValueFormatter
        ),
        modifier = modifier
    )
}
