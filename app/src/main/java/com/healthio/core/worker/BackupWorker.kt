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
import com.google.api.services.sheets.v4.model.AddSheetRequest
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import com.healthio.core.data.dataStore
import com.healthio.core.database.AppDatabase
import com.healthio.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Collections

class BackupWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.fastingDao()

    override suspend fun doWork(): Result {
        try {
            // 1. Check Auth
            val email = context.dataStore.data.map { preferences ->
                preferences[SettingsViewModel.GOOGLE_ACCOUNT_EMAIL]
            }.first() ?: return Result.success() // Not connected

            // 2. Get Unsynced Logs
            val unsyncedLogs = dao.getUnsyncedLogs()
            if (unsyncedLogs.isEmpty()) return Result.success()

            // 3. Setup Sheets Service
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(SheetsScopes.DRIVE_FILE, SheetsScopes.SPREADSHEETS)
            ).setSelectedAccountName(email)

            val service = Sheets.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Healthio").build()

            // 4. Find or Create Spreadsheet
            val spreadsheetId = getOrCreateSpreadsheetId(service)

            // 5. Append Logs
            unsyncedLogs.forEach { log ->
                val startZone = Instant.ofEpochMilli(log.startTime).atZone(ZoneId.systemDefault())
                val year = startZone.year.toString()
                
                ensureSheetExists(service, spreadsheetId, year)
                
                val dateStr = startZone.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val startStr = startZone.format(DateTimeFormatter.ISO_LOCAL_TIME)
                val endStr = Instant.ofEpochMilli(log.endTime).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_TIME)
                val hours = log.durationMillis / (1000.0 * 60 * 60)
                
                val values = listOf(
                    listOf(dateStr, startStr, endStr, String.format("%.2f", hours))
                )
                
                val body = ValueRange().setValues(values)
                service.spreadsheets().values().append(spreadsheetId, "$year!A1", body)
                    .setValueInputOption("USER_ENTERED")
                    .execute()
            }

            // 6. Mark as Synced
            dao.markAsSynced(unsyncedLogs.map { it.id })

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
            .setProperties(SpreadsheetProperties().setTitle("Healthio Fasting Logs"))
        
        val created = service.spreadsheets().create(spreadsheet).execute()
        val newId = created.spreadsheetId
        
        context.dataStore.edit { it[SettingsViewModel.SPREADSHEET_ID] = newId }
        return newId
    }
    
    private fun ensureSheetExists(service: Sheets, spreadsheetId: String, title: String) {
        val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
        val sheet = spreadsheet.sheets.find { it.properties.title == title }
        if (sheet == null) {
            val request = Request().setAddSheet(
                AddSheetRequest().setProperties(
                    SheetProperties().setTitle(title)
                )
            )
            val batchRequest = BatchUpdateSpreadsheetRequest().setRequests(listOf(request))
            service.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute()
            
            // Add Header Row
            val header = ValueRange().setValues(listOf(listOf("Date", "Start", "End", "Hours")))
            service.spreadsheets().values().append(spreadsheetId, "$title!A1", header)
                .setValueInputOption("USER_ENTERED")
                .execute()
        }
    }
}