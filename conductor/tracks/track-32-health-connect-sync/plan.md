# Implementation Plan - Track 32: Health Connect Sync

- [x] **Dependencies:**
    - [x] Verify `HealthConnectManager` is ready.
- [x] **ViewModel Logic:**
    - [x] Update `WorkoutViewModel` to inject `HealthConnectManager`.
    - [x] Implement `syncFromHealthConnect()` function:
        -   Check permissions.
        -   Fetch workouts for `LocalDate.now()`.
        -   Filter out duplicates using `externalId`.
        -   Save new workouts to repository.
- [x] **UI Update:**
    - [x] Modify `AddWorkoutDialog.kt`:
        -   Add a "Sync with Health Connect" button.
        -   Wire it to `viewModel.syncFromHealthConnect()`.
- [x] **Permissions:**
    - [x] Ensure `HomeScreen.kt` handles the permission launcher.
    - [x] Update `AndroidManifest.xml` with Health Connect permissions.
