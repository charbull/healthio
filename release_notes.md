# Release Notes - v1.0.5

## v1.0.5 - Critical Bug Fixes & Optimization (2026-02-05)

### 📊 Accuracy & Logic Fixes
*   **Calorie Accuracy:** Fixed a major bug where Health Connect data was being double-counted. Calories from individual workouts are now correctly subtracted from the daily active burn total.
*   **Real-time Stats:** Improved the reliability of the live calorie and timer updates on the Home Screen to prevent UI flickers and race conditions.

### 🛠️ Sync & Infrastructure
*   **Google Sheets Optimization:** Refactored the backup engine to use batch updates. This makes syncing faster and much more reliable, especially for users with a lot of unsynced data.
*   **Atomic Syncing:** Data is now only marked as "synced" after a confirmed successful write to Google Sheets, preventing potential data loss or duplicates during poor network conditions.

---

## v1.0.4 - Sync Reliability & Manual Control (2026-02-05)
...
