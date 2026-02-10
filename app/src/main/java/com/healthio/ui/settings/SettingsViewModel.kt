package com.healthio.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthio.core.data.dataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
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
        val DAILY_CARBS_GOAL = intPreferencesKey("daily_carbs_goal")
        val DAILY_FAT_GOAL = intPreferencesKey("daily_fat_goal")
        val PROTEIN_CALC_METHOD = stringPreferencesKey("protein_calc_method")
        val PROTEIN_FIXED_GOAL = intPreferencesKey("protein_fixed_goal")
        val PROTEIN_MULTIPLIER = floatPreferencesKey("protein_multiplier")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            context.dataStore.data.map { preferences ->
                SettingsUiState(
                    connectedEmail = preferences[GOOGLE_ACCOUNT_EMAIL],
                    isConnected = !preferences[GOOGLE_ACCOUNT_EMAIL].isNullOrEmpty(),
                    geminiApiKey = preferences[GEMINI_API_KEY],
                    baseDailyBurn = preferences[BASE_DAILY_BURN] ?: 1800,
                    carbsGoal = preferences[DAILY_CARBS_GOAL] ?: 50,
                    fatGoal = preferences[DAILY_FAT_GOAL] ?: 130,
                    proteinMethod = preferences[PROTEIN_CALC_METHOD] ?: "MULTIPLIER",
                    proteinFixedGoal = preferences[PROTEIN_FIXED_GOAL] ?: 150,
                    proteinMultiplier = preferences[PROTEIN_MULTIPLIER] ?: 0.8f,
                    weightUnit = preferences[WEIGHT_UNIT] ?: "LBS",
                    onboardingCompleted = preferences[ONBOARDING_COMPLETED] ?: false
                )
            }.collect { state ->
                _uiState.value = state
                if (state.isConnected) {
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

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
        }
    }

    fun setWeightUnit(unit: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[WEIGHT_UNIT] = unit }
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
            triggerManualSync()
        }
    }

    fun setCarbsGoal(goal: Int) {
        viewModelScope.launch { context.dataStore.edit { it[DAILY_CARBS_GOAL] = goal } }
    }

    fun setFatGoal(goal: Int) {
        viewModelScope.launch { context.dataStore.edit { it[DAILY_FAT_GOAL] = goal } }
    }

    fun setProteinMethod(method: String) {
        viewModelScope.launch { context.dataStore.edit { it[PROTEIN_CALC_METHOD] = method } }
    }

    fun setProteinFixedGoal(goal: Int) {
        viewModelScope.launch { context.dataStore.edit { it[PROTEIN_FIXED_GOAL] = goal } }
    }

    fun setProteinMultiplier(mult: Float) {
        viewModelScope.launch { context.dataStore.edit { it[PROTEIN_MULTIPLIER] = mult } }
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

    fun triggerManualSync() {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<BackupWorker>()
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    private fun scheduleBackup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        // Target 11:00 PM
        val now = java.time.LocalDateTime.now()
        var target = now.withHour(23).withMinute(0).withSecond(0).withNano(0)
        
        if (now.isAfter(target)) {
            target = target.plusDays(1)
        }
        
        val initialDelay = java.time.Duration.between(now, target).toMinutes()

        val workRequest = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "BackupWorker",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}

data class SettingsUiState(
    val isConnected: Boolean = false,
    val connectedEmail: String? = null,
    val geminiApiKey: String? = null,
    val baseDailyBurn: Int = 1800,
    val carbsGoal: Int = 50,
    val fatGoal: Int = 130,
    val proteinMethod: String = "MULTIPLIER",
    val proteinFixedGoal: Int = 150,
    val proteinMultiplier: Float = 0.8f,
    val weightUnit: String = "LBS",
    val onboardingCompleted: Boolean = false
)
