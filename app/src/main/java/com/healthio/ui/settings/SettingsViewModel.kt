package com.healthio.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthio.core.data.dataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
        val BASE_DAILY_BURN = intPreferencesKey("base_daily_burn")
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            context.dataStore.data.map { preferences ->
                val email = preferences[GOOGLE_ACCOUNT_EMAIL]
                val apiKey = preferences[GEMINI_API_KEY]
                val baseBurn = preferences[BASE_DAILY_BURN] ?: 1800 // Default 1800
                
                Triple(email, apiKey, baseBurn)
            }.collect { (email, apiKey, baseBurn) ->
                _uiState.value = _uiState.value.copy(
                    connectedEmail = email,
                    isConnected = !email.isNullOrEmpty(),
                    geminiApiKey = apiKey,
                    baseDailyBurn = baseBurn
                )
                if (!email.isNullOrEmpty()) {
                    scheduleBackup()
                }
            }
        }
    }

    fun setBaseBurn(burn: Int) {
        viewModelScope.launch {
            context.dataStore.edit { it[BASE_DAILY_BURN] = burn }
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
    val geminiApiKey: String? = null,
    val baseDailyBurn: Int = 1800
)
