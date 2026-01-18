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
fun FastingChart(
    entries: List<ChartEntry>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val chartEntryModelProducer = remember(entries) { ChartEntryModelProducer(entries) }
    
    val horizontalAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        labels.getOrNull(value.toInt()) ?: ""
    }

    Chart(
        chart = columnChart(
            columns = listOf(
                lineComponent(
                    color = Color(0xFF4CAF50),
                    thickness = 12.dp, // Slightly thinner for monthly views
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
