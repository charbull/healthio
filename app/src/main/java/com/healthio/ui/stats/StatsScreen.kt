package com.healthio.ui.stats

import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val timeRange by viewModel.timeRange.collectAsState()
    val statType by viewModel.statType.collectAsState()
    val chartSeries by viewModel.chartSeries.collectAsState()
    val chartLabels by viewModel.chartLabels.collectAsState()
    val workoutCount by viewModel.workoutCount.collectAsState()
    val totalCalories by viewModel.totalCalories.collectAsState()

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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stat Type Toggle
            TabRow(
                selectedTabIndex = statType.ordinal,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                StatType.values().forEach { type ->
                    Tab(
                        selected = statType == type,
                        onClick = { viewModel.setStatType(type) },
                        text = { Text(type.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

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

            val title = when (statType) {
                StatType.Fasting -> "Fasting Hours"
                StatType.Workouts -> "Exercises"
                StatType.Calories -> "Energy (kcal)"
                StatType.Macros -> "Macros (Grams)"
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Legend for Macros
            if (statType == StatType.Macros) {
                Row(modifier = Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendItem("Protein", Color(0xFF2196F3))
                    LegendItem("Carbs", Color(0xFFFFC107))
                    LegendItem("Fat", Color(0xFFE91E63))
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (chartSeries.isEmpty()) {
                Text("No data for this period.", style = MaterialTheme.typography.bodyLarge)
            } else {
                HealthioChart(
                    series = chartSeries,
                    labels = chartLabels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val label = when (statType) {
                            StatType.Fasting -> "Tracking status"
                            StatType.Workouts -> "Active consistency"
                            else -> "Total Consumption"
                        }
                        val value = when (statType) {
                            StatType.Fasting -> "Active"
                            StatType.Workouts -> "$workoutCount sessions logged"
                            else -> "$totalCalories kcal total"
                        }
                        
                        Text(text = label, style = MaterialTheme.typography.labelMedium)
                        Text(text = value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(12.dp), color = color, shape = androidx.compose.foundation.shape.CircleShape) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
