package com.healthio.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.healthio.core.database.AppDatabase
import com.healthio.core.database.FastingLog

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class FastingRepository(private val context: Context) {
    companion object {
        val IS_FASTING = booleanPreferencesKey("is_fasting")
        val START_TIME = longPreferencesKey("start_time")
    }

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.fastingDao()

    val isFasting: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_FASTING] ?: false
        }

    val startTime: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[START_TIME]
        }
    
    val fastingHistory: Flow<List<FastingLog>> = dao.getAllLogs()

    suspend fun startFast(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[IS_FASTING] = true
            preferences[START_TIME] = timestamp
        }
    }

    suspend fun endFast() {
        context.dataStore.edit { preferences ->
            preferences[IS_FASTING] = false
            preferences.remove(START_TIME)
        }
    }
    
    suspend fun logCompletedFast(start: Long, end: Long) {
        val duration = (end - start).coerceAtLeast(0L)
        dao.insertLog(FastingLog(startTime = start, endTime = end, durationMillis = duration))
    }
}
