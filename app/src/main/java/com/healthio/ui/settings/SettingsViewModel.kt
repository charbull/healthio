package com.healthio.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthio.core.data.dataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.healthio.core.worker.BackupWorker
import java.util.concurrent.TimeUnit

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    
    companion object {
        val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        val SPREADSHEET_ID = stringPreferencesKey("spreadsheet_id")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            context.dataStore.data.map { preferences ->
                Triple(
                    preferences[GOOGLE_ACCOUNT_EMAIL],
                    preferences[GEMINI_API_KEY],
                    preferences[SPREADSHEET_ID]
                )
            }.collect { (email, apiKey, _) ->
                _uiState.value = _uiState.value.copy(
                    connectedEmail = email,
                    isConnected = !email.isNullOrEmpty(),
                    geminiApiKey = apiKey
                )
                if (!email.isNullOrEmpty()) {
                    scheduleBackup()
                }
            }
        }
    }

    fun setGeminiApiKey(key: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[GEMINI_API_KEY] = key }
        }
    }

    fun setConnectedAccount(email: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[GOOGLE_ACCOUNT_EMAIL] = email
            }
        }
    }

    fun disconnectAccount() {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences.remove(GOOGLE_ACCOUNT_EMAIL)
                preferences.remove(SPREADSHEET_ID)
            }
            WorkManager.getInstance(context).cancelUniqueWork("BackupWorker")
        }
    }

    private fun scheduleBackup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "BackupWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

data class SettingsUiState(
    val isConnected: Boolean = false,
    val connectedEmail: String? = null,
    val geminiApiKey: String? = null
)
