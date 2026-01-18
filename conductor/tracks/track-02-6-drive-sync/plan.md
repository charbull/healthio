# Implementation Plan - Track 02.6

## Phase 1: Dependencies & Data
- [x] Add WorkManager and Google Client libraries to `app/build.gradle.kts`.
- [x] Update `FastingLog` with `isSynced` column.
- [x] Update `AppDatabase` version and migration (or destructive fallback for MVP).
- [x] Update `FastingDao`.

## Phase 2: Authentication
- [x] Create `SettingsScreen` composable.
- [x] Implement Google Sign-In logic using `Credential Manager` or `play-services-auth`.
- [x] Save auth state/tokens securely (or rely on silent sign-in).

## Phase 3: Sheets Service
- [x] Create `GoogleSheetsRepository` (Integrated into BackupWorker).
- [x] Implement `createSpreadsheet`, `ensureYearSheet`, `appendLogs`.

## Phase 4: WorkManager
- [x] Create `BackupWorker`.
- [x] Configure `WorkRequest` with `RequiresCharging` and `NetworkType.UNMETERED`.
- [x] Enqueue unique work.

## Phase 5: UI & Integration
- [x] Add Settings button to Home Screen.
- [x] Wire up "Connect" button.
- [x] Verify flow.
