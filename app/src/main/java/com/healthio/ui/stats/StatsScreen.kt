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
    val chartEntries by viewModel.chartEntries.collectAsState()
    val chartLabels by viewModel.chartLabels.collectAsState()
    val workoutCount by viewModel.workoutCount.collectAsState()

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
            // Stat Type Toggle (Fasting vs Workouts)
            TabRow(
                selectedTabIndex = StatType.values().indexOf(statType),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                StatType.values().forEach { type ->
                    Tab(
                        selected = statType == type,
                        onClick = { viewModel.setStatType(type) },
                        text = { Text(type.name) }
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

            val title = if (statType == StatType.Fasting) {
                when (timeRange) {
                    TimeRange.Week -> "Fasting Hours (Last 7 Days)"
                    TimeRange.Month -> "Daily Max (This Month)"
                    TimeRange.Year -> "Avg Daily Fast (This Year)"
                }
            } else {
                "Workouts / sessions"
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (chartEntries.isEmpty()) {
                Text("No data for this period.", style = MaterialTheme.typography.bodyLarge)
            } else {
                FastingChart(
                    entries = chartEntries,
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
                        Text(
                            text = "Activity",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "$workoutCount sessions logged",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}