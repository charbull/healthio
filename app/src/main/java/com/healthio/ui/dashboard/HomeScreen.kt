package com.healthio.ui.dashboard

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthio.ui.components.FastCompletedDialog
import com.healthio.ui.components.FluxTimer
import com.healthio.ui.settings.SettingsViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Dialog Integration
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

    // Calendar for picking date/time
    val calendar = Calendar.getInstance()

    // Time Picker
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            
            val selectedTime = calendar.timeInMillis
            val now = System.currentTimeMillis()
            
            viewModel.startFastAt(if (selectedTime > now) now else selectedTime)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false 
    )

    // Date Picker
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            timePickerDialog.show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    
    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
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
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.BottomEnd),
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
            
            // Stats & Settings Buttons
            Row {
                IconButton(onClick = onNavigateToStats) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "History"
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        }

        // Center: Timer
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FluxTimer(
                state = uiState.timerState,
                elapsedMillis = uiState.elapsedMillis
            )
            
            Spacer(modifier = Modifier.size(32.dp))
            
            Text(
                text = uiState.timeDisplay,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Light,
                    fontFeatureSettings = "tnum"
                )
            )
            
            Text(
                text = if (uiState.timerState == TimerState.FASTING) "FASTING TIME" else "READY",
                color = if (uiState.timerState == TimerState.FASTING) Color(0xFF4CAF50) else Color(0xFFFF9800),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "END FAST",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                Button(
                    onClick = { viewModel.startFastNow() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "START FASTING NOW",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                TextButton(onClick = { datePickerDialog.show() }) {
                    Text(
                        text = "Start from specific time...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    }
}