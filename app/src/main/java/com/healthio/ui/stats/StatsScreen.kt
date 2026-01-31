package com.healthio.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthio.ui.components.EditMealDialog

import com.healthio.core.database.MealLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val macroSeries by viewModel.macroSeries.collectAsState()
    val weightSeries by viewModel.weightSeries.collectAsState()
    val weightUnit by viewModel.weightUnit.collectAsState()
    val recentMeals by viewModel.recentMeals.collectAsState()
    val summaryTitle by viewModel.summaryTitle.collectAsState()
    val summaryValue by viewModel.summaryValue.collectAsState()
    val workoutDetails by viewModel.workoutDetails.collectAsState()
    
    var selectedMealToEdit by remember { mutableStateOf<MealLog?>(null) }

    if (selectedMealToEdit != null) {
        EditMealDialog(
            meal = selectedMealToEdit!!,
            onDismiss = { selectedMealToEdit = null },
            onUpdate = { updatedMeal ->
                viewModel.updateMeal(updatedMeal)
                selectedMealToEdit = null
            },
            onDelete = { mealToDelete ->
                viewModel.deleteMeal(mealToDelete)
                selectedMealToEdit = null
            }
        )
    }

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
            // Stat Type Toggle
            val tabs = listOf(StatType.Intake, StatType.Workouts, StatType.Fasting)
            TabRow(
                selectedTabIndex = tabs.indexOf(statType).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEach { type ->
                    Tab(
                        selected = statType == type,
                        onClick = { viewModel.setStatType(type) },
                        text = { 
                            Text(
                                text = type.name,
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        }
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
                StatType.Fasting -> "Fasting Time (Hours)"
                StatType.Workouts -> "Workout Intensity (kcal/min)"
                StatType.Intake -> "Energy Balance (kcal)"
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (statType == StatType.Intake) {
                // No legend for single series Net Energy
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (chartSeries.isEmpty()) {
                Text("No data for this period.", style = MaterialTheme.typography.bodyLarge)
            } else {
                key(chartSeries, chartLabels) {
                    HealthioChart(
                        series = chartSeries,
                        labels = chartLabels,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        overrideColors = null // Use default colors (Red/Green for Intake)
                    )
                }
            }

            if (statType == StatType.Intake && macroSeries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Macronutrients (Percentage)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(modifier = Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendItem("Protein", Color(0xFF2196F3))
                    LegendItem("Carbs", Color(0xFFFFC107))
                    LegendItem("Fat", Color(0xFFE91E63))
                }
                HealthioChart(
                    series = macroSeries,
                    labels = chartLabels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Summary Card
            if (summaryTitle.isNotEmpty() || summaryValue.isNotEmpty() || (statType == StatType.Workouts && workoutDetails != null)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (statType == StatType.Workouts && workoutDetails != null) {
                            Text(text = "Workout Summary", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                SummaryDetail("Sessions", "${workoutDetails?.sessions}")
                                SummaryDetail("Burned", "${workoutDetails?.calories} kcal")
                                SummaryDetail("Duration", "${workoutDetails?.minutes} min")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Frequency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                SummaryDetail("This Week", "${workoutDetails?.sessionsWeek}")
                                SummaryDetail("This Month", "${workoutDetails?.sessionsMonth}")
                                SummaryDetail("This Year", "${workoutDetails?.sessionsYear}")
                            }
                        } else {
                            Text(text = summaryTitle, style = MaterialTheme.typography.labelMedium)
                            Text(text = summaryValue, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
            
            if (statType == StatType.Intake && weightSeries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Weight Trend (${weightUnit.lowercase()})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HealthioChart(
                    series = weightSeries,
                    labels = chartLabels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    overrideColors = listOf(Color(0xFF9C27B0)), // Purple
                    isLineChart = true
                )
            }

            if (statType == StatType.Intake && recentMeals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Recent Meals (Last 30 Days)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp).align(Alignment.Start)
                )
                MealHistoryList(recentMeals, onMealClick = { selectedMealToEdit = it })
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MealHistoryList(meals: List<MealLog>, onMealClick: (MealLog) -> Unit) {
    val groupedMeals = remember(meals) {
        val zoneId = java.time.ZoneId.systemDefault()
        meals.sortedByDescending { it.timestamp }
            .groupBy { 
                java.time.Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate() 
            }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        groupedMeals.forEach { (date, dailyMeals) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MealHeader(date)
                dailyMeals.forEach { meal ->
                    MealHistoryItem(meal, onClick = { onMealClick(meal) })
                }
            }
        }
    }
}

@Composable
fun MealHeader(date: java.time.LocalDate) {
    val today = java.time.LocalDate.now()
    val label = when {
        date.isEqual(today) -> "Today"
        date.isEqual(today.minusDays(1)) -> "Yesterday"
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM dd"))
    }
    
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun MealHistoryItem(meal: MealLog, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = meal.foodName, 
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                val date = Date(meal.timestamp)
                val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                Text(text = format.format(date), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${meal.calories} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(text = " • ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                Text(text = "Prot: ${meal.protein}g", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2196F3))
                Text(text = " • ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                Text(text = "Carb: ${meal.carbs}g", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFFC107))
                Text(text = " • ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                Text(text = "Fat: ${meal.fat}g", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE91E63))
            }
            if (meal.insulinScore > 0 || meal.fiber > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Keto Impact: ${meal.carbs - meal.fiber}g Net Carbs • Insulin Score: ${meal.insulinScore}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (meal.insulinScore < 40) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }
    }
}

@Composable
fun SummaryDetail(label: String, value: String) {
    Column {
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value, 
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
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
