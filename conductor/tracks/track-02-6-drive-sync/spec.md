# Track 02.6: Google Drive Sync

**Goal:** Auto-backup fasting logs to a Google Sheet when charging and on Wi-Fi.

## Scope
1.  **Dependencies:**
    -   `androidx.work:work-runtime-ktx`
    -   `com.google.android.gms:play-services-auth`
    -   `com.google.api-client:google-api-client-android`
    -   `com.google.apis:google-api-services-sheets`
2.  **Data Layer:**
    -   Update `FastingLog` entity with `isSynced: Boolean`.
    -   Update `FastingDao` to `getUnsyncedLogs()` and `markAsSynced()`.
3.  **Auth & Settings:**
    -   Create `SettingsScreen` with "Connect Google Account" button.
    -   Handle OAuth flow with `https://www.googleapis.com/auth/drive.file` scope.
4.  **Sync Logic (SyncWorker):**
    -   Check constraints (Charging + Wi-Fi).
    -   Initialize Sheets Service.
    -   Ensure "Healthio Logs" sheet exists.
    -   Ensure Year tab exists.
    -   Append logs.
    -   Update local DB.

## Success Criteria
-   User can sign in with Google.
-   Logs appear in a Google Sheet in the user's Drive.
-   Sheet is organized by Year tabs.
-   Sync only happens when conditions (Charging + Wi-Fi) are met (or triggered manually for testing).
