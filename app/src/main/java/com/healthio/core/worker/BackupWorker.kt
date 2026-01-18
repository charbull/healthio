package com.healthio.core.worker

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import com.healthio.core.data.dataStore
import com.healthio.core.database.AppDatabase
import com.healthio.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BackupWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getDatabase(context)
    private val fastingDao = db.fastingDao()
    private val mealDao = db.mealDao()
    private val workoutDao = db.workoutDao()

    override suspend fun doWork(): Result {
        try {
            val email = context.dataStore.data.map { preferences ->
                preferences[SettingsViewModel.GOOGLE_ACCOUNT_EMAIL]
            }.first() ?: return Result.success()

            val unsyncedFasting = fastingDao.getUnsyncedLogs()
            val unsyncedMeals = mealDao.getUnsyncedMeals()
            val unsyncedWorkouts = workoutDao.getUnsyncedWorkouts()

            if (unsyncedFasting.isEmpty() && unsyncedMeals.isEmpty() && unsyncedWorkouts.isEmpty()) {
                return Result.success()
            }

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(SheetsScopes.DRIVE_FILE, SheetsScopes.SPREADSHEETS)
            ).setSelectedAccountName(email)

            val service = Sheets.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Healthio").build()

            val spreadsheetId = getOrCreateSpreadsheetId(service)

            // 1. Sync Fasting Logs
            if (unsyncedFasting.isNotEmpty()) {
                unsyncedFasting.forEach { log ->
                    val startZone = Instant.ofEpochMilli(log.startTime).atZone(ZoneId.systemDefault())
                    val tabName = "${startZone.year}_Fasting"
                    ensureSheetExists(service, spreadsheetId, tabName, listOf("Date", "Start", "End", "Hours"))
                    val values = listOf(listOf(
                        startZone.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        startZone.format(DateTimeFormatter.ISO_LOCAL_TIME),
                        Instant.ofEpochMilli(log.endTime).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_TIME),
                        String.format("%.2f", log.durationMillis / (1000.0 * 60 * 60))
                    ))
                    appendRow(service, spreadsheetId, tabName, values)
                }
                fastingDao.markAsSynced(unsyncedFasting.map { it.id })
            }

            // 2. Sync Meal Logs
            if (unsyncedMeals.isNotEmpty()) {
                unsyncedMeals.forEach { meal ->
                    val time = Instant.ofEpochMilli(meal.timestamp).atZone(ZoneId.systemDefault())
                    val tabName = "${time.year}_Meals"
                    ensureSheetExists(service, spreadsheetId, tabName, listOf("Date", "Time", "Food", "Calories", "Protein", "Carbs", "Fat"))
                    val values = listOf(listOf(
                        time.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        time.format(DateTimeFormatter.ISO_LOCAL_TIME),
                        meal.foodName,
                        meal.calories.toString(),
                        meal.protein.toString(),
                        meal.carbs.toString(),
                        meal.fat.toString()
                    ))
                    appendRow(service, spreadsheetId, tabName, values)
                }
                mealDao.markAsSynced(unsyncedMeals.map { it.id })
            }

            // 3. Sync Workouts
            if (unsyncedWorkouts.isNotEmpty()) {
                unsyncedWorkouts.forEach { workout ->
                    val time = Instant.ofEpochMilli(workout.timestamp).atZone(ZoneId.systemDefault())
                    val tabName = "${time.year}_Workouts"
                    ensureSheetExists(service, spreadsheetId, tabName, listOf("Date", "Time", "Type", "Calories", "DurationMin"))
                    val values = listOf(listOf(
                        time.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        time.format(DateTimeFormatter.ISO_LOCAL_TIME),
                        workout.type,
                        workout.calories.toString(),
                        workout.durationMinutes.toString()
                    ))
                    appendRow(service, spreadsheetId, tabName, values)
                }
                workoutDao.markAsSynced(unsyncedWorkouts.map { it.id })
            }

            return Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private suspend fun getOrCreateSpreadsheetId(service: Sheets): String {
        val storedId = context.dataStore.data.map { it[SettingsViewModel.SPREADSHEET_ID] }.first()
        if (!storedId.isNullOrEmpty()) return storedId

        val spreadsheet = Spreadsheet()
            .setProperties(SpreadsheetProperties().setTitle("Healthio Dashboard Data"))
        
        val created = service.spreadsheets().create(spreadsheet).execute()
        val newId = created.spreadsheetId
        
        context.dataStore.edit { it[SettingsViewModel.SPREADSHEET_ID] = newId }
        return newId
    }
    
    private fun ensureSheetExists(service: Sheets, spreadsheetId: String, title: String, headers: List<String>) {
        val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
        val sheet = spreadsheet.sheets.find { it.properties.title == title }
        if (sheet == null) {
            val request = Request().setAddSheet(
                AddSheetRequest().setProperties(SheetProperties().setTitle(title))
            )
            service.spreadsheets().batchUpdate(spreadsheetId, BatchUpdateSpreadsheetRequest().setRequests(listOf(request))).execute()
            appendRow(service, spreadsheetId, title, listOf(headers))
        }
    }

    private fun appendRow(service: Sheets, spreadsheetId: String, range: String, values: List<List<String>>) {
        val body = ValueRange().setValues(values)
        service.spreadsheets().values().append(spreadsheetId, "$range!A1", body)
            .setValueInputOption("USER_ENTERED")
            .execute()
    }
}