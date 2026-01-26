package com.healthio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.healthio.core.database.MealLog

@Composable
fun EditMealDialog(
    meal: MealLog,
    onDismiss: () -> Unit,
    onUpdate: (MealLog) -> Unit,
    onDelete: (MealLog) -> Unit
) {
    var name by remember { mutableStateOf(meal.foodName) }
    var calories by remember { mutableStateOf(meal.calories.toString()) }
    var protein by remember { mutableStateOf(meal.protein.toString()) }
    var carbs by remember { mutableStateOf(meal.carbs.toString()) }
    var fat by remember { mutableStateOf(meal.fat.toString()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Meal") },
            text = { Text("Are you sure you want to delete this meal?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(meal) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Meal") },
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
                            onUpdate(
                                meal.copy(
                                    foodName = name,
                                    calories = calories.toIntOrNull() ?: 0,
                                    protein = protein.toIntOrNull() ?: 0,
                                    carbs = carbs.toIntOrNull() ?: 0,
                                    fat = fat.toIntOrNull() ?: 0
                                )
                            )
                        }
                    },
                    enabled = name.isNotBlank() && calories.isNotBlank()
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDeleteConfirmation = true }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
