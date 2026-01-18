package com.healthio.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.sheets.v4.SheetsScopes

import com.google.android.gms.common.api.ApiException
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(SheetsScopes.DRIVE_FILE), Scope(SheetsScopes.SPREADSHEETS))
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Attempt to get account regardless of result code, as it might contain the error info
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.email?.let { email ->
                viewModel.setConnectedAccount(email)
                Toast.makeText(context, "Connected: $email", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            e.printStackTrace()
            val errorMsg = when (e.statusCode) {
                10 -> "Error 10: Configuration. Check SHA-1 in Cloud Console."
                12500 -> "Error 12500: SHA-1 Mismatch or Missing Support Email."
                12501 -> "User Cancelled."
                else -> "Sign-In Failed: Code ${e.statusCode}"
            }
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Backup & Sync",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.isConnected) {
                        Text(
                            text = "Connected as:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = uiState.connectedEmail ?: "",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                googleSignInClient.signOut().addOnCompleteListener {
                                    viewModel.disconnectAccount()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Disconnect")
                        }
                    } else {
                        Text(
                            text = "Connect Google Drive to auto-backup your fasting logs.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = { launcher.launch(googleSignInClient.signInIntent) }
                        ) {
                            Text("Connect Google Drive")
                        }
                    }
                }
            }

            Text(
                text = "AI Configuration",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Gemini API Key",
                        style = MaterialTheme.typography.labelLarge
                    )
                    OutlinedTextField(
                        value = uiState.geminiApiKey ?: "",
                        onValueChange = { viewModel.setGeminiApiKey(it) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        placeholder = { Text("Paste your API Key here") },
                        singleLine = true
                    )
                    
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    TextButton(
                        onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") }
                    ) {
                        Text("Get a free API Key from Google AI Studio")
                    }
                }
            }
        }
    }
}
