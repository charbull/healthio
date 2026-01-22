package com.healthio.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AddWorkoutDialog(
    onDismiss: () -> Unit,
    onManualLog: (String, Int, Int, Long) -> Unit,
    onSyncHealthConnect: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("Running") }
    var duration by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var timestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    
    val types = listOf("Running", "Biking", "Resistance", "Other")
    
    val dateStr = remember(timestamp) {
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onSyncHealthConnect != null) {
                    OutlinedButton(
                        onClick = {
                            onSyncHealthConnect()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sync from Health Connect")
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Or Manual Entry", style = MaterialTheme.typography.labelMedium)
                }

                Text("Activity Type", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                
                // Date/Time Display & Picker
                OutlinedCard(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = timestamp
                        DatePickerDialog(context, { _, y, m, d ->
                            calendar.set(y, m, d)
                            TimePickerDialog(context, { _, hh, mm ->
                                calendar.set(Calendar.HOUR_OF_DAY, hh)
                                calendar.set(Calendar.MINUTE, mm)
                                timestamp = calendar.timeInMillis
                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = dateStr, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Calories burned") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onManualLog(type, duration.toIntOrNull() ?: 0, calories.toIntOrNull() ?: 0, timestamp)
                    onDismiss()
                },
                enabled = duration.isNotEmpty() && calories.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
