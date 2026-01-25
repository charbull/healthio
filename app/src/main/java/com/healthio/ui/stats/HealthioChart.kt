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
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.chart.column.ColumnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer

@Composable
fun HealthioChart(
    series: List<List<ChartEntry>>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    overrideColors: List<Color>? = null,
    isLineChart: Boolean = false
) {
    if (series.isEmpty()) return
    
    val chartEntryModelProducer = remember(series) { ChartEntryModelProducer(series) }
    
    val horizontalAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        labels.getOrNull(value.toInt()) ?: ""
    }

    // Define colors for series
    val colors = listOf(
        Color(0xFF4CAF50), // Green (Default)
        Color(0xFFFFC107), // Yellow (Carbs)
        Color(0xFFE91E63), // Pink (Fat)
        Color(0xFF2196F3)  // Blue (Protein)
    )

    val isMacros = series.size == 3
    val isIntake = series.size == 2
    
    // Adjust colors based on series count
    val columnColors = overrideColors ?: when (series.size) {
        3 -> listOf(Color(0xFF2196F3), Color(0xFFFFC107), Color(0xFFE91E63)) // Macros: P, C, F
        2 -> listOf(Color(0xFFF44336), Color(0xFF4CAF50)) // Intake: Positive (Red), Negative (Green)
        else -> listOf(colors[0])
    }

    val pointCount = series.firstOrNull()?.size ?: 0
    val isDense = pointCount > 20

    val barThickness = when {
        isDense -> 6.dp
        else -> 12.dp
    }
    
    val barSpacing = when {
        isDense -> 4.dp
        else -> 12.dp
    }

    val maxValue = if (isMacros) {
        // For stacked, max is the sum of Ys at the same index
        val sums = mutableMapOf<Int, Float>()
        series.forEach { s ->
            s.forEach { entry ->
                sums[entry.x.toInt()] = (sums[entry.x.toInt()] ?: 0f) + entry.y
            }
        }
        sums.values.maxOrNull() ?: 0f
    } else {
        series.flatten().maxByOrNull { it.y }?.y ?: 0f
    }

    val verticalItemPlacer = com.patrykandpatrick.vico.core.axis.AxisItemPlacer.Vertical.default(
        maxItemCount = when {
            isMacros -> 6 // 0, 20, 40, 60, 80, 100
            maxValue <= 0f -> 5
            maxValue <= 5f -> (maxValue.toInt() + 1).coerceAtLeast(2)
            else -> 5
        }
    )

    val chart = if (isLineChart) {
        lineChart(
            lines = columnColors.map { color ->
                com.patrykandpatrick.vico.core.chart.line.LineChart.LineSpec(
                    lineColor = color.hashCode(),
                    lineThicknessDp = 3f
                )
            }
        )
    } else {
        columnChart(
            columns = columnColors.map { color ->
                lineComponent(
                    color = color,
                    thickness = barThickness,
                    shape = if (isMacros || isIntake) Shapes.rectShape else Shapes.roundedCornerShape(topLeftPercent = 50, topRightPercent = 50)
                )
            },
            spacing = barSpacing,
            mergeMode = if (isMacros || isIntake) ColumnChart.MergeMode.Stack else ColumnChart.MergeMode.Grouped
        )
    }

    Chart(
        chart = chart,
        chartModelProducer = chartEntryModelProducer,
        startAxis = rememberStartAxis(
            valueFormatter = { value, _ -> 
                if (isMacros) "${value.toInt()}%" else String.format("%.0f", value) 
            },
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
