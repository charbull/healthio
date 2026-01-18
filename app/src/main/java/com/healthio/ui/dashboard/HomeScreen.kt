package com.healthio.ui.dashboard

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
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
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthio.ui.components.AddWorkoutDialog
import com.healthio.ui.components.FastCompletedDialog
import com.healthio.ui.components.FluxTimer
import com.healthio.ui.components.ManualEntryTypeDialog
import com.healthio.ui.settings.SettingsViewModel
import com.healthio.ui.workouts.WorkoutSyncState
import com.healthio.ui.workouts.WorkoutViewModel
import java.util.Calendar

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

    // State for Dialogs
    var showEntryTypeDialog by remember { mutableStateOf(false) }
    var showWorkoutDialog by remember { mutableStateOf(false) }
    var showInstallHCDialog by remember { mutableStateOf(false) }
    var tempStartTime by remember { mutableStateOf(0L) }

    val hcPermissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    val permissionsLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(hcPermissions)) {
            workoutViewModel.fetchFromHealthConnect()
        } else {
            Toast.makeText(context, "Permissions Denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Sync State Effect
    LaunchedEffect(workoutSyncState) {
        when (workoutSyncState) {
            is WorkoutSyncState.NeedsPermission -> {
                permissionsLauncher.launch(hcPermissions)
                workoutViewModel.resetSyncState()
            }
            is WorkoutSyncState.Success -> {
                val count = (workoutSyncState as WorkoutSyncState.Success).count
                Toast.makeText(context, "Imported $count workouts", Toast.LENGTH_SHORT).show()
                workoutViewModel.resetSyncState()
            }
            is WorkoutSyncState.Error -> {
                val error = (workoutSyncState as WorkoutSyncState.Error).message
                if (error.contains("NOT installed", ignoreCase = true) || error.contains("Code 1") || error.contains("Code 2")) {
                    showInstallHCDialog = true
                } else {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
                workoutViewModel.resetSyncState()
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
            onFetchFromHealthConnect = {
                showWorkoutDialog = false
                workoutViewModel.fetchFromHealthConnect()
            },
            onManualLog = { type, duration, calories ->
                workoutViewModel.logManualWorkout(type, duration, calories)
            }
        )
    }

    if (showInstallHCDialog) {
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showInstallHCDialog = false },
            title = { Text("Health Connect Required") },
            text = { Text("To sync data from Garmin, Strava, or other apps, you need to install Google Health Connect.") },
            confirmButton = {
                Button(onClick = { 
                    showInstallHCDialog = false
                    uriHandler.openUri("market://details?id=com.google.android.apps.healthdata")
                }) {
                    Text("Install from Play Store")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallHCDialog = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToVision,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Scan Food")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
                        text = "HEALTHIO",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp
                        )
                    )
                }
                Row {
                    IconButton(onClick = onNavigateToStats) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "History")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }

            // Center: Timer & Summary
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FluxTimer(state = uiState.timerState, elapsedMillis = uiState.elapsedMillis)
                Spacer(modifier = Modifier.size(32.dp))
                Text(
                    text = uiState.timeDisplay,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Light, fontFeatureSettings = "tnum"
                    )
                )
                Text(
                    text = if (uiState.timerState == TimerState.FASTING) "FASTING TIME" else "READY",
                    color = if (uiState.timerState == TimerState.FASTING) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Summary Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Today's Energy",
                                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { showWorkoutDialog = true }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Workout", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
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
                    }
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.timerState == TimerState.FASTING) {
                    Button(
                        onClick = { viewModel.requestEndFast() },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = "END FAST", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                } else {
                    Button(
                        onClick = { viewModel.startFastNow() },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = "START FASTING NOW", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    TextButton(onClick = { showEntryTypeDialog = true }) {
                        Text(text = "Manual Entry / Log Past Fast...", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)))
                    }
                }
            }
        }
    }
}