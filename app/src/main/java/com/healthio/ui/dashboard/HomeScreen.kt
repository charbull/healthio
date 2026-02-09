package com.healthio.ui.dashboard

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthio.ui.components.AddMealDialog
import com.healthio.ui.components.AddWorkoutDialog
import com.healthio.ui.components.FastCompletedDialog
import com.healthio.ui.components.FluxTimer
import com.healthio.ui.components.ManualEntryTypeDialog
import com.healthio.ui.components.ManualFoodLogDialog
import com.healthio.ui.components.AddWeightDialog
import com.healthio.ui.components.OnboardingDialog
import com.healthio.ui.settings.SettingsViewModel
import com.healthio.ui.workouts.WorkoutSyncState
import com.healthio.ui.workouts.WorkoutViewModel
import java.util.Calendar

import androidx.health.connect.client.PermissionController
import com.healthio.core.health.HealthConnectManager

@Composable
fun HomeScreen(
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToVision: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    workoutViewModel: WorkoutViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val workoutSyncState by workoutViewModel.syncState.collectAsState()
    val context = LocalContext.current

    // Health Connect Permission Launcher
    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        val required = HealthConnectManager(context).permissions
        if (granted.containsAll(required)) { 
            workoutViewModel.syncFromHealthConnect() 
        } else {
            Toast.makeText(context, "Permissions missing. Opening Settings...", Toast.LENGTH_LONG).show()
            // Fallback: Open Health Connect settings specifically or App Settings
            try {
                // Try specific Health Connect settings
                val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    // Fallback to App Details
                    val appIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(appIntent)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to open settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission for Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        // Auto-sync if permissions granted
        val hcManager = HealthConnectManager(context)
        if (hcManager.hasPermissions()) {
            workoutViewModel.syncFromHealthConnect()
        }
    }

    // State for Dialogs
    var showEntryTypeDialog by remember { mutableStateOf(false) }
    var showWorkoutDialog by remember { mutableStateOf(false) }
    var showAddMealDialog by remember { mutableStateOf(false) }
    var showManualMealDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var forceShowOnboarding by remember { mutableStateOf(false) }
    var tempStartTime by remember { mutableStateOf(0L) }

    if (!settingsState.onboardingCompleted || forceShowOnboarding) {
        OnboardingDialog(
            onDismiss = {
                forceShowOnboarding = false
                settingsViewModel.setOnboardingCompleted(true)
            }
        )
    }

    // Sync State Feedback
    LaunchedEffect(workoutSyncState) {
        when (workoutSyncState) {
            is WorkoutSyncState.Success -> {
                Toast.makeText(context, (workoutSyncState as WorkoutSyncState.Success).message, Toast.LENGTH_SHORT).show()
                workoutViewModel.resetSyncState()
            }
            is WorkoutSyncState.Error -> {
                Toast.makeText(context, (workoutSyncState as WorkoutSyncState.Error).message, Toast.LENGTH_LONG).show()
                workoutViewModel.resetSyncState()
            }
            is WorkoutSyncState.PermissionRequired -> {
                val permissions = HealthConnectManager(context).permissions
                healthConnectPermissionLauncher.launch(permissions)
                workoutViewModel.resetSyncState()
            }
            is WorkoutSyncState.Loading -> {
                Toast.makeText(context, "Syncing...", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    // Dialogs
    if (uiState.showFeedbackDialog) {
        FastCompletedDialog(
            duration = uiState.completedDuration,
            quote = uiState.feedbackQuote,
            onDismiss = { 
                viewModel.confirmEndFast()
                onNavigateToStats()
            }
        )
    }

    if (showEntryTypeDialog) {
        ManualEntryTypeDialog(
            onDismiss = { showEntryTypeDialog = false },
            onOngoingSelected = {
                showEntryTypeDialog = false
                val calendar = Calendar.getInstance()
                DatePickerDialog(context, { _, year, month, day ->
                    calendar.set(year, month, day)
                    TimePickerDialog(context, { _, hour, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)
                        val time = calendar.timeInMillis
                        val now = System.currentTimeMillis()
                        viewModel.startFastAt(if (time > now) now else time)
                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            },
            onCompletedSelected = {
                showEntryTypeDialog = false
                val calendar = Calendar.getInstance()
                DatePickerDialog(context, { _, year, month, day ->
                    calendar.set(year, month, day)
                    TimePickerDialog(context, { _, hour, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)
                        tempStartTime = calendar.timeInMillis
                        val endCalendar = Calendar.getInstance()
                        endCalendar.timeInMillis = tempStartTime
                        DatePickerDialog(context, { _, endYear, endMonth, endDay ->
                            endCalendar.set(endYear, endMonth, endDay)
                            TimePickerDialog(context, { _, endHour, endMinute ->
                                endCalendar.set(Calendar.HOUR_OF_DAY, endHour)
                                endCalendar.set(Calendar.MINUTE, endMinute)
                                viewModel.logPastFast(tempStartTime, endCalendar.timeInMillis)
                                onNavigateToStats() 
                            }, endCalendar.get(Calendar.HOUR_OF_DAY), endCalendar.get(Calendar.MINUTE), false).show()
                        }, endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH), endCalendar.get(Calendar.DAY_OF_MONTH)).show()
                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }
        )
    }

    if (showWorkoutDialog) {
        AddWorkoutDialog(
            onDismiss = { showWorkoutDialog = false },
            onManualLog = { type, duration, calories, ts ->
                workoutViewModel.logManualWorkout(type, duration, calories, ts)
            },
            onSyncHealthConnect = {
                workoutViewModel.syncFromHealthConnect()
            }
        )
    }
    
    if (showAddMealDialog) {
        AddMealDialog(
            onDismiss = { showAddMealDialog = false },
            onScanSelected = {
                showAddMealDialog = false
                onNavigateToVision()
            },
            onManualSelected = {
                showAddMealDialog = false
                showManualMealDialog = true
            }
        )
    }
    
    if (showManualMealDialog) {
        ManualFoodLogDialog(
            onDismiss = { showManualMealDialog = false },
            onLog = { name, calories, protein, carbs, fat ->
                viewModel.logManualMeal(name, calories, protein, carbs, fat)
                showManualMealDialog = false
                Toast.makeText(context, "Meal Logged", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showWeightDialog) {
        AddWeightDialog(
            onDismiss = { showWeightDialog = false },
            onSave = { weight ->
                viewModel.logManualWeight(weight)
                showWeightDialog = false
                Toast.makeText(context, "Weight Updated", Toast.LENGTH_SHORT).show()
            },
            onSync = {
                workoutViewModel.syncFromHealthConnect()
            },
            unit = uiState.weightUnit
        )
    }

    Scaffold(
        // FAB removed as requested
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.healthio.R.drawable.ic_healthio_logo),
                            contentDescription = "Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp)
                        )
                        if (settingsState.isConnected) {
                            Surface(
                                modifier = Modifier.size(10.dp).align(Alignment.BottomEnd),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = Color(0xFF4CAF50),
                                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.background)
                            ) {}
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Healthio",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Row {
                    IconButton(onClick = { forceShowOnboarding = true }) {
                        Icon(imageVector = Icons.Default.HelpOutline, contentDescription = "Guide")
                    }
                    IconButton(onClick = onNavigateToStats) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "History")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }

            // Section 1: Fasting Control
            FastingSection(
                uiState = uiState,
                onStartFast = { viewModel.startFastNow() },
                onEndFast = { viewModel.requestEndFast() },
                onManualEntry = { showEntryTypeDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))

            // Macro Widget
            MacroSection(uiState = uiState)

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Energy Dashboard
            EnergySection(
                uiState = uiState,
                onAddWorkout = { showWorkoutDialog = true },
                onAddMeal = { showAddMealDialog = true }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Weight Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Current Weight", style = MaterialTheme.typography.labelMedium)
                        val weightText = if (uiState.currentWeight != null) {
                            if (uiState.weightUnit == "LBS") {
                                String.format("%.1f lbs", uiState.currentWeight!! * 2.20462f)
                            } else {
                                String.format("%.1f kg", uiState.currentWeight!!)
                            }
                        } else {
                            if (uiState.weightUnit == "LBS") "-- lbs" else "-- kg"
                        }
                        Text(
                            text = weightText,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showWeightDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Weight", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Weight",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ... (Rest of file remains unchanged, checking length)
@Composable
fun FastingSection(
    uiState: HomeUiState,
    onStartFast: () -> Unit,
    onEndFast: () -> Unit,
    onManualEntry: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            FluxTimer(
                state = uiState.timerState,
                elapsedMillis = uiState.elapsedMillis,
                timeDisplay = uiState.timeDisplay
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (uiState.timerState == TimerState.FASTING) {
            Button(
                onClick = onEndFast,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = "END FAST", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        } else {
            Button(
                onClick = onStartFast,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = "START FASTING", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            TextButton(onClick = onManualEntry, modifier = Modifier.padding(top = 4.dp)) {
                Text(text = "Manual Entry / Log Past Fast...", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)))
            }
        }
    }
}

@Composable
fun MacroSection(uiState: HomeUiState) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Today's Macros",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Protein
                val proteinGoal = uiState.proteinGoal
                val proteinProgress = if (proteinGoal > 0) uiState.todayProtein.toFloat() / proteinGoal else 0f
                val proteinColor = when {
                    proteinProgress >= 0.9f -> Color(0xFF4CAF50) // Green when close to or over goal
                    else -> Color(0xFF2196F3) // Blue default
                }
                MacroProgressBar(
                    label = "Protein",
                    value = uiState.todayProtein,
                    goal = proteinGoal,
                    color = proteinColor,
                    unit = "g"
                )

                // Carbs
                val carbGoal = uiState.carbsGoal
                val carbProgress = if (carbGoal > 0) uiState.todayCarbs.toFloat() / carbGoal else 0f
                val carbColor = when {
                    carbProgress >= 0.8f -> Color(0xFFF44336) // Red when close to or over limit
                    else -> Color(0xFFFFC107) // Yellow default
                }
                MacroProgressBar(
                    label = "Carbs",
                    value = uiState.todayCarbs,
                    goal = carbGoal,
                    color = carbColor,
                    unit = "g"
                )

                // Fat
                MacroProgressBar(
                    label = "Fat",
                    value = uiState.todayFat,
                    goal = uiState.fatGoal,
                    color = Color(0xFFE91E63),
                    unit = "g"
                )
            }
        }
    }
}

@Composable
fun MacroProgressBar(
    label: String,
    value: Int,
    goal: Int,
    color: Color,
    unit: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "$value / $goal $unit",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = (value.toFloat() / goal).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun EnergySection(
    uiState: HomeUiState,
    onAddWorkout: () -> Unit,
    onAddMeal: () -> Unit
) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Today's Energy",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header (No Icon)
                Text(
                    text = "Calories",
                    style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "${uiState.todayCalories}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(text = "Intake", style = MaterialTheme.typography.labelSmall)
                    }
                    Text("-", style = MaterialTheme.typography.titleLarge)
                    Column {
                        Text(text = "${uiState.todayBurnedCalories}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFFE91E63))
                        Text(text = "Burned", style = MaterialTheme.typography.labelSmall)
                    }
                    Text("=", style = MaterialTheme.typography.titleLarge)
                    Column {
                        val net = uiState.todayCalories - uiState.todayBurnedCalories
                        Text(text = "$net", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = if (net <= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface)
                        Text(text = "Net kcal", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilledTonalButton(
                        onClick = onAddMeal,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Meal")
                    }
                    
                    FilledTonalButton(
                        onClick = onAddWorkout,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Workout")
                    }
                }
            }
        }
    }
}
