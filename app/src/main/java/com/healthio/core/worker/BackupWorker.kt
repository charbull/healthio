package com.healthio.core.worker

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import android.util.Log
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

    companion object {
        private const val TAG = "BackupWorker"
    }

    private val db = AppDatabase.getDatabase(context)
    private val fastingDao = db.fastingDao()
    private val mealDao = db.mealDao()
    private val workoutDao = db.workoutDao()
    private val weightDao = db.weightDao()

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting backup work...")
        try {
            val email = context.dataStore.data.map { preferences ->
                preferences[SettingsViewModel.GOOGLE_ACCOUNT_EMAIL]
            }.first() ?: run {
                Log.d(TAG, "No Google account connected, skipping.")
                return Result.success()
            }

            Log.d(TAG, "Connected account: $email")

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(SheetsScopes.DRIVE_FILE, "https://www.googleapis.com/auth/spreadsheets")
            ).setSelectedAccountName(email)

            val service = Sheets.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Healthio").build()

            val (spreadsheetId, isNewDiscovery) = try {
                getOrCreateSpreadsheetId(service, credential)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get/create spreadsheet", e)
                if (e is GoogleJsonResponseException && (e.statusCode == 401 || e.statusCode == 403)) {
                    // Auth error, might need user intervention
                    return Result.failure()
                }
                return Result.retry()
            }

            Log.d(TAG, "Using spreadsheetId: $spreadsheetId (New discovery: $isNewDiscovery)")

            if (isNewDiscovery) {
                Log.d(TAG, "New discovery, pulling from spreadsheet...")
                pullFromSpreadsheet(service, spreadsheetId)
            }

            // Ensure at least the current year's Fasting tab exists so user sees SOMETHING in their Drive
            // This also validates the spreadsheet is still writable.
            val currentYear = java.time.LocalDate.now().year
            ensureSheetExists(service, spreadsheetId, "${currentYear}_Fasting", listOf("Date", "Start", "End", "Hours", "RawTimestamp"))

            val unsyncedFasting = fastingDao.getUnsyncedLogs()
            val unsyncedMeals = mealDao.getUnsyncedMeals()
            val unsyncedWorkouts = workoutDao.getUnsyncedWorkouts()
            val unsyncedWeights = weightDao.getUnsyncedWeights()

            Log.d(TAG, "Unsynced data: Fasting=${unsyncedFasting.size}, Meals=${unsyncedMeals.size}, Workouts=${unsyncedWorkouts.size}, Weights=${unsyncedWeights.size}")

            if (unsyncedFasting.isEmpty() && unsyncedMeals.isEmpty() && unsyncedWorkouts.isEmpty() && unsyncedWeights.isEmpty()) {
                Log.d(TAG, "No unsynced data, structure verified.")
                return Result.success()
            }

            // 1. Sync Fasting Logs
            if (unsyncedFasting.isNotEmpty()) {
                val groups = unsyncedFasting.groupBy { log ->
                    Instant.ofEpochMilli(log.startTime).atZone(ZoneId.systemDefault()).year
                }
                groups.forEach { (year, logs) ->
                    val tabName = "${year}_Fasting"
                    if (ensureSheetExists(service, spreadsheetId, tabName, listOf("Date", "Start", "End", "Hours", "RawTimestamp"))) {
                        val rows = logs.map { log ->
                            val startZone = Instant.ofEpochMilli(log.startTime).atZone(ZoneId.systemDefault())
                            val endZone = Instant.ofEpochMilli(log.endTime).atZone(ZoneId.systemDefault())
                            listOf(
                                startZone.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                startZone.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                endZone.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                String.format("%.2f", log.durationMillis / (1000.0 * 60 * 60)),
                                log.startTime.toString()
                            )
                        }
                        if (appendRows(service, spreadsheetId, tabName, rows)) {
                            fastingDao.markAsSynced(logs.map { it.id })
                        }
                    }
                }
            }

            // 2. Sync Meal Logs
            if (unsyncedMeals.isNotEmpty()) {
                val groups = unsyncedMeals.groupBy { meal ->
                    Instant.ofEpochMilli(meal.timestamp).atZone(ZoneId.systemDefault()).year
                }
                groups.forEach { (year, meals) ->
                    val tabName = "${year}_Meals"
                    if (ensureSheetExists(service, spreadsheetId, tabName, listOf("Date", "Time", "Food", "Calories", "Protein", "Carbs", "Fat", "RawTimestamp"))) {
                        val rows = meals.map { meal ->
                            val time = Instant.ofEpochMilli(meal.timestamp).atZone(ZoneId.systemDefault())
                            listOf(
                                time.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                time.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                meal.foodName,
                                meal.calories.toString(),
                                meal.protein.toString(),
                                meal.carbs.toString(),
                                meal.fat.toString(),
                                meal.timestamp.toString()
                            )
                        }
                        if (appendRows(service, spreadsheetId, tabName, rows)) {
                            mealDao.markAsSynced(meals.map { it.id })
                        }
                    }
                }
            }

            // 3. Sync Workouts
            if (unsyncedWorkouts.isNotEmpty()) {
                val groups = unsyncedWorkouts.groupBy { workout ->
                    Instant.ofEpochMilli(workout.timestamp).atZone(ZoneId.systemDefault()).year
                }
                groups.forEach { (year, workouts) ->
                    val tabName = "${year}_Workouts"
                    if (ensureSheetExists(service, spreadsheetId, tabName, listOf("Date", "Time", "Type", "Calories", "DurationMin", "RawTimestamp"))) {
                        val rows = workouts.map { workout ->
                            val time = Instant.ofEpochMilli(workout.timestamp).atZone(ZoneId.systemDefault())
                            listOf(
                                time.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                time.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                workout.type,
                                workout.calories.toString(),
                                workout.durationMinutes.toString(),
                                workout.timestamp.toString()
                            )
                        }
                        if (appendRows(service, spreadsheetId, tabName, rows)) {
                            workoutDao.markAsSynced(workouts.map { it.id })
                        }
                    }
                }
            }

            // 4. Sync Weights
            if (unsyncedWeights.isNotEmpty()) {
                val groups = unsyncedWeights.groupBy { weight ->
                    Instant.ofEpochMilli(weight.timestamp).atZone(ZoneId.systemDefault()).year
                }
                groups.forEach { (year, weights) ->
                    val tabName = "${year}_Weight"
                    if (ensureSheetExists(service, spreadsheetId, tabName, listOf("Date", "Time", "WeightKg", "RawTimestamp"))) {
                        val rows = weights.map { weight ->
                            val time = Instant.ofEpochMilli(weight.timestamp).atZone(ZoneId.systemDefault())
                            listOf(
                                time.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                time.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                weight.valueKg.toString(),
                                weight.timestamp.toString()
                            )
                        }
                        if (appendRows(service, spreadsheetId, tabName, rows)) {
                            weightDao.markAsSynced(weights.map { it.id })
                        }
                    }
                }
            }

            return Result.success()

        } catch (e: UserRecoverableAuthException) {
            e.printStackTrace()
            // We can't show UI from a worker, so we just fail and wait for user to open app
            return Result.failure()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private suspend fun pullFromSpreadsheet(service: Sheets, spreadsheetId: String) {
        try {
            val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
            spreadsheet.sheets.forEach { sheet ->
                val title = sheet.properties.title
                val response = service.spreadsheets().values().get(spreadsheetId, title).execute()
                val rows = response.getValues() ?: return@forEach
                if (rows.size < 2) return@forEach // Only header or empty

                val headers = rows[0].map { it.toString() }
                val timestampIdx = headers.indexOf("RawTimestamp")
                if (timestampIdx == -1) return@forEach

                val dataRows = rows.drop(1)

                when {
                    title.endsWith("_Fasting") -> {
                        dataRows.forEach { row ->
                            SpreadsheetParser.parseFastingRow(row, timestampIdx)?.let {
                                fastingDao.insertLog(it)
                            }
                        }
                    }
                    title.endsWith("_Meals") -> {
                        dataRows.forEach { row ->
                            SpreadsheetParser.parseMealRow(row, timestampIdx)?.let {
                                mealDao.insertMeal(it)
                            }
                        }
                    }
                    title.endsWith("_Workouts") -> {
                        dataRows.forEach { row ->
                            SpreadsheetParser.parseWorkoutRow(row, timestampIdx)?.let {
                                workoutDao.insertWorkout(it)
                            }
                        }
                    }
                    title.endsWith("_Weight") -> {
                        dataRows.forEach { row ->
                            SpreadsheetParser.parseWeightRow(row, timestampIdx)?.let {
                                weightDao.insertWeight(it)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun getOrCreateSpreadsheetId(service: Sheets, credential: GoogleAccountCredential): Pair<String, Boolean> {
        val storedId = context.dataStore.data.map { it[SettingsViewModel.SPREADSHEET_ID] }.first()
        
        // 1. If we have a stored ID, verify it still exists and is accessible
        if (!storedId.isNullOrEmpty()) {
            Log.d(TAG, "Checking stored spreadsheetId: $storedId")
            try {
                service.spreadsheets().get(storedId).execute()
                Log.d(TAG, "Stored spreadsheet is valid.")
                return Pair(storedId, false)
            } catch (e: Exception) {
                Log.w(TAG, "Stored spreadsheetId invalid or inaccessible: $storedId")
                // If 404 or forbidden, we might have lost access or it was deleted
                if (e is GoogleJsonResponseException && (e.statusCode == 404 || e.statusCode == 403)) {
                    context.dataStore.edit { it.remove(SettingsViewModel.SPREADSHEET_ID) }
                } else {
                    throw e
                }
            }
        }

        // 2. Always try to find existing spreadsheet by name first to avoid duplicates
        Log.d(TAG, "Searching Drive for existing 'Healthio Dashboard Data'...")
        val driveService = com.google.api.services.drive.Drive.Builder(
            com.google.api.client.http.javanet.NetHttpTransport(),
            com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Healthio").build()

        val files = try {
            driveService.files().list()
                .setQ("name contains 'Healthio' and mimeType = 'application/vnd.google-apps.spreadsheet' and trashed = false")
                .setFields("files(id, name)")
                .execute().files
        } catch (e: Exception) {
            Log.e(TAG, "Drive search failed", e)
            null
        }

        if (!files.isNullOrEmpty()) {
            val existingId = files[0].id
            Log.d(TAG, "Found existing spreadsheet in Drive: $existingId")
            context.dataStore.edit { it[SettingsViewModel.SPREADSHEET_ID] = existingId }
            return Pair(existingId, true) // isNewDiscovery = true to trigger a pull from cloud
        }

        // 3. Only create if absolutely nothing was found
        Log.d(TAG, "Creating new spreadsheet 'Healthio Dashboard Data'...")
        val spreadsheet = Spreadsheet()
            .setProperties(SpreadsheetProperties().setTitle("Healthio Dashboard Data"))
        
        val created = service.spreadsheets().create(spreadsheet).execute()
        val newId = created.spreadsheetId
        Log.d(TAG, "Created new spreadsheet with ID: $newId")
        
        context.dataStore.edit { it[SettingsViewModel.SPREADSHEET_ID] = newId }
        return Pair(newId, false)
    }
    
    private fun ensureSheetExists(service: Sheets, spreadsheetId: String, title: String, headers: List<String>): Boolean {
        return try {
            val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
            val sheet = spreadsheet.sheets.find { it.properties.title == title }
            if (sheet == null) {
                val request = Request().setAddSheet(
                    AddSheetRequest().setProperties(SheetProperties().setTitle(title))
                )
                service.spreadsheets().batchUpdate(spreadsheetId, BatchUpdateSpreadsheetRequest().setRequests(listOf(request))).execute()
                appendRows(service, spreadsheetId, title, listOf(headers))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun appendRows(service: Sheets, spreadsheetId: String, range: String, values: List<List<String>>): Boolean {
        return try {
            val body = ValueRange().setValues(values)
            service.spreadsheets().values().append(spreadsheetId, "$range!A1", body)
                .setValueInputOption("USER_ENTERED")
                .execute()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}