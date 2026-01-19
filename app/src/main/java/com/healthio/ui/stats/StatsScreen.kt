package com.healthio.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.core.entry.ChartEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val timeRange by viewModel.timeRange.collectAsState()
    val chartLabels by viewModel.chartLabels.collectAsState()
    
    val fastingState by viewModel.fastingState.collectAsState()
    val energyState by viewModel.energyState.collectAsState()
    val nutritionState by viewModel.nutritionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Range Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimeRange.values().forEach { range ->
                    FilterChip(
                        selected = range == timeRange,
                        onClick = { viewModel.setTimeRange(range) },
                        label = { Text(range.name) }
                    )
                }
            }

            // 1. Fasting Section
            StatsSection(
                title = "Fasting Consistency",
                chartSeries = fastingState.chartSeries,
                labels = chartLabels,
                summaryContent = {
                    SummaryDetail("Consistency", fastingState.summaryValue)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 2. Energy & Workouts Section
            StatsSection(
                title = "Energy & Workout",
                chartSeries = energyState.chartSeries,
                labels = chartLabels,
                legendContent = {
                     Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendItem("Intake", Color(0xFF4CAF50))
                        LegendItem("Burned", Color(0xFFFFC107))
                    }
                },
                summaryContent = {
                    val details = energyState.workoutSummary
                    if (details != null) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                SummaryDetail("Sessions", "${details.sessions}")
                                SummaryDetail("Burned", "${details.calories} kcal")
                                SummaryDetail("Duration", "${details.minutes} min")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                SummaryDetail("Wk Freq", "${details.sessionsWeek}")
                                SummaryDetail("Mo Freq", "${details.sessionsMonth}")
                                SummaryDetail("Yr Freq", "${details.sessionsYear}")
                            }
                        }
                    } else {
                        Text("No workout data")
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Nutrition Section
            StatsSection(
                title = "Food Intake (Macros)",
                chartSeries = nutritionState.chartSeries,
                labels = chartLabels,
                legendContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendItem("Protein", Color(0xFF2196F3))
                        LegendItem("Carbs", Color(0xFFFFC107))
                        LegendItem("Fat", Color(0xFFE91E63))
                    }
                },
                summaryContent = {
                    SummaryDetail("Total Intake", nutritionState.summaryValue)
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatsSection(
    title: String,
    chartSeries: List<List<ChartEntry>>,
    labels: List<String>,
    legendContent: @Composable (() -> Unit)? = null,
    summaryContent: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            
            if (legendContent != null) {
                Spacer(modifier = Modifier.height(8.dp))
                legendContent()
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (chartSeries.isEmpty() || chartSeries.all { it.isEmpty() }) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("No data available", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            } else {
                HealthioChart(
                    series = chartSeries,
                    labels = labels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    summaryContent()
                }
            }
        }
    }
}

@Composable
fun SummaryDetail(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(8.dp), color = color, shape = androidx.compose.foundation.shape.CircleShape) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}