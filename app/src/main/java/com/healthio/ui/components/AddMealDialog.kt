package com.healthio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddMealDialog(
    onDismiss: () -> Unit,
    onScanSelected: () -> Unit,
    onManualSelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Meal") },
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onScanSelected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scan Food (AI)")
                }
                OutlinedButton(
                    onClick = onManualSelected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manual Entry")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            // No explicit dismiss button needed if we have actions, but maybe a cancel
        }
    )
}
