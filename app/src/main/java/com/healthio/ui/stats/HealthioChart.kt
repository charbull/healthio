package com.healthio.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

@Composable
fun HealthioChart(
    series: List<List<ChartEntry>>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    if (series.isEmpty()) return
    
    val chartEntryModelProducer = remember(series) { ChartEntryModelProducer(series) }
    
    val horizontalAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        labels.getOrNull(value.toInt()) ?: ""
    }

    // Define colors for series
    val colors = listOf(
        Color(0xFF4CAF50), // Green (Default/Protein in grouped)
        Color(0xFFFFC107), // Yellow (Carbs)
        Color(0xFFE91E63), // Pink (Fat)
        Color(0xFF2196F3)  // Blue
    )

    // Adjust colors if it's the Macros view (3 series)
    val columnColors = if (series.size == 3) {
        listOf(Color(0xFF2196F3), Color(0xFFFFC107), Color(0xFFE91E63)) // P, C, F
    } else {
        listOf(colors[0])
    }

    val pointCount = series.firstOrNull()?.size ?: 0
    val isDense = pointCount > 20
    val barThickness = if (isDense) {
        if (series.size > 1) 2.dp else 6.dp
    } else {
        if (series.size > 1) 4.dp else 12.dp
    }
    val barSpacing = if (isDense) 4.dp else if (series.size > 1) 4.dp else 12.dp

    val maxValue = series.flatten().maxByOrNull { it.y }?.y ?: 0f
    val verticalItemPlacer = com.patrykandpatrick.vico.core.axis.AxisItemPlacer.Vertical.default(
        maxItemCount = when {
            maxValue <= 0f -> 5
            maxValue <= 5f -> (maxValue.toInt() + 1).coerceAtLeast(2)
            else -> 5
        }
    )

    Chart(
        chart = columnChart(
            columns = columnColors.map { color ->
                lineComponent(
                    color = color,
                    thickness = barThickness,
                    shape = Shapes.roundedCornerShape(topLeftPercent = 50, topRightPercent = 50)
                )
            },
            spacing = barSpacing
        ),
        chartModelProducer = chartEntryModelProducer,
        startAxis = rememberStartAxis(
            valueFormatter = { value, _ -> String.format("%.0f", value) },
            itemPlacer = verticalItemPlacer,
            guideline = null
        ),
        bottomAxis = rememberBottomAxis(
            valueFormatter = horizontalAxisValueFormatter,
            guideline = null,
            itemPlacer = com.patrykandpatrick.vico.core.axis.AxisItemPlacer.Horizontal.default(
                spacing = if (isDense) 5 else 1,
                offset = 0,
                shiftExtremeTicks = false
            )
        ),
        modifier = modifier
    )
}
