package com.healthio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ManualFoodLogDialog(
    onDismiss: () -> Unit,
    onLog: (String, Int, Int, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Meal Manually") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { if (it.all { char -> char.isDigit() }) calories = it },
                    label = { Text("Calories (kcal) *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = protein,
                    onValueChange = { if (it.all { char -> char.isDigit() }) protein = it },
                    label = { Text("Protein (g)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { if (it.all { char -> char.isDigit() }) carbs = it },
                    label = { Text("Carbs (g)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { if (it.all { char -> char.isDigit() }) fat = it },
                    label = { Text("Fat (g)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && calories.isNotBlank()) {
                        onLog(
                            name,
                            calories.toIntOrNull() ?: 0,
                            protein.toIntOrNull() ?: 0,
                            carbs.toIntOrNull() ?: 0,
                            fat.toIntOrNull() ?: 0
                        )
                    }
                },
                enabled = name.isNotBlank() && calories.isNotBlank()
            ) {
                Text("Log Meal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
