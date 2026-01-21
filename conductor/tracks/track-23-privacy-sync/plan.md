# Implementation Plan - Track 23: Privacy-Focused Sync

- [x] **Audit Scopes:**
    - [x] Locate Google Sign-In initialization (likely in `SettingsViewModel` or a repository).
    - [x] Update scopes to list *only* `SheetsScopes.DRIVE_FILE`.
    - [x] Update `BackupWorker.kt` credential setup to match.
- [x] **UI Update:**
    - [x] Modify `SettingsScreen.kt`.
    - [x] Add a "Privacy Note" section near the Google Sync button explaining the scope.
    - [x] Alternatively, show a one-time dialog when the user clicks "Connect Google Account".
- [x] **Test Sync:**
    - [x] Clear app data / Revoke access.
    - [x] Re-link account.
    - [x] Verify a new spreadsheet is created and data is appended.