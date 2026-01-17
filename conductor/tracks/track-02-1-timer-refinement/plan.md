# Implementation Plan - Track 02.1

## Phase 1: Data Persistence
- [x] Add `androidx.datastore:datastore-preferences` to `app/build.gradle.kts`.
- [x] Create `com.healthio.core.data.FastingRepository`.
- [x] Implement `saveFastStart`, `getFastState`, `clearFast`.

## Phase 2: Logic Refinement
- [x] Update `HomeUiState` to include `startTime` and `isFasting`.
- [x] Update `HomeViewModel`:
    -   Use a `Ticker` (Coroutines) to update progress every minute.
    -   Implement logic for "Start Now" vs "Start at Time".

## Phase 3: UI Enhancements
- [x] Add "Set Start Time" button to `HomeScreen`.
- [x] Add `Material3` Time/Date Picker dialog for manual entry.
- [x] Connect UI actions to ViewModel.

## Phase 4: Validation
- [x] Run app, start fast, restart app -> Verify fast is still active.
- [x] Start fast at past time -> Verify progress ring is partially full.
