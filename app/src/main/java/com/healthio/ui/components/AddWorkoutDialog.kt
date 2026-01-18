package com.healthio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddWorkoutDialog(
    onDismiss: () -> Unit,
    onFetchFromHealthConnect: () -> Unit,
    onManualLog: (String, Int, Int) -> Unit
) {
    var showManualForm by remember { mutableStateOf(false) }

    if (!showManualForm) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Log Workout") },
            text = { Text("Import from Garmin/Health Connect or enter manually?") },
            confirmButton = {
                Button(onClick = onFetchFromHealthConnect) {
                    Text("Fetch from Health Connect")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showManualForm = true }) {
                    Text("Manual Entry")
                }
            }
        )
    } else {
        var type by remember { mutableStateOf("Running") }
        var duration by remember { mutableStateOf("") }
        var calories by remember { mutableStateOf("") }
        val types = listOf("Running", "Biking", "Resistance", "Other")

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Manual Workout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Activity Type", style = MaterialTheme.typography.labelLarge)
                    // Simple Dropdown approximation
                    types.forEach { t ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = type == t, onClick = { type = t })
                            Text(t)
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
                        onManualLog(type, duration.toIntOrNull() ?: 0, calories.toIntOrNull() ?: 0)
                        onDismiss()
                    },
                    enabled = duration.isNotEmpty() && calories.isNotEmpty()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualForm = false }) {
                    Text("Back")
                }
            }
        )
    }
}
