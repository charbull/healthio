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
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        
        val dailyDurations = mutableMapOf<Int, MutableList<Float>>()
        for (i in 1..7) dailyDurations[i] = mutableListOf()

        logs.forEach { log ->
            var currentStart = Instant.ofEpochMilli(log.startTime).atZone(zoneId)
            val endDateTime = Instant.ofEpochMilli(log.endTime).atZone(zoneId)
            
            while (currentStart.isBefore(endDateTime)) {
                val endOfDay = currentStart.toLocalDate().plusDays(1).atStartOfDay(zoneId)
                val segmentEnd = if (endDateTime.isBefore(endOfDay)) endDateTime else endOfDay
                
                val durationMillis = ChronoUnit.MILLIS.between(currentStart, segmentEnd)
                val hours = (durationMillis / (1000f * 60 * 60)).coerceAtLeast(0f)
                val dayOfWeek = currentStart.dayOfWeek.value 
                
                val logDate = currentStart.toLocalDate()
                val daysDiff = ChronoUnit.DAYS.between(logDate, today)
                if (daysDiff < 7 && daysDiff >= 0) {
                     dailyDurations[dayOfWeek]?.add(hours)
                }
                currentStart = segmentEnd
            }
        }
        
        val chartEntries = mutableListOf<ChartEntry>()
        for (i in 1..7) {
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
                    shape = Shapes.roundedCornerShape(
                        topLeftPercent = 50,
                        topRightPercent = 50,
                        bottomLeftPercent = 0,
                        bottomRightPercent = 0
                    )
                )
            )
        ),
        chartModelProducer = chartEntryModelProducer,
        startAxis = rememberStartAxis(
            valueFormatter = { value, _ -> String.format("%.0fh", value) },
            itemPlacer = com.patrykandpatrick.vico.core.axis.AxisItemPlacer.Vertical.default(maxItemCount = 5)
        ),
        bottomAxis = rememberBottomAxis(
            valueFormatter = horizontalAxisValueFormatter,
            guideline = null
        ),
        modifier = modifier
    )
}