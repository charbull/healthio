# Implementation Plan - Track 23: Privacy-Focused Sync

- [ ] **Audit Scopes:**
    - [ ] Locate Google Sign-In initialization (likely in `SettingsViewModel` or a repository).
    - [ ] Update scopes to list *only* `SheetsScopes.DRIVE_FILE`.
    - [ ] Update `BackupWorker.kt` credential setup to match.
- [ ] **UI Update:**
    - [ ] Modify `SettingsScreen.kt`.
    - [ ] Add a "Privacy Note" section near the Google Sync button explaining the scope.
    - [ ] Alternatively, show a one-time dialog when the user clicks "Connect Google Account".
- [ ] **Test Sync:**
    - [ ] Clear app data / Revoke access.
    - [ ] Re-link account.
    - [ ] Verify a new spreadsheet is created and data is appended.
