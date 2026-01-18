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
    val chartEntries by viewModel.chartEntries.collectAsState()
    val chartLabels by viewModel.chartLabels.collectAsState()

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

            Text(
                text = when (timeRange) {
                    TimeRange.Week -> "Fasting Hours (Last 7 Days)"
                    TimeRange.Month -> "Daily Max (This Month)"
                    TimeRange.Year -> "Avg Daily Fast (This Year)"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (chartEntries.isEmpty()) {
                Text("No fasting logs yet.", style = MaterialTheme.typography.bodyLarge)
            } else {
                FastingChart(
                    entries = chartEntries,
                    labels = chartLabels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }
        }
    }
}