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
fun ManualEntryTypeDialog(
    onDismiss: () -> Unit,
    onOngoingSelected: () -> Unit,
    onCompletedSelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Entry") },
        text = { Text("Are you starting a fast that is still ongoing, or logging one you already finished?") },
        confirmButton = {
            Button(onClick = onCompletedSelected) {
                Text("Log Completed Fast")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onOngoingSelected) {
                Text("Start Ongoing Fast")
            }
        }
    )
}
