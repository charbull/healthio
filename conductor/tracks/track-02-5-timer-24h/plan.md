# Implementation Plan - Track 02.5

## Phase 1: ViewModel Updates
- [x] Update `HomeUiState` to hold `elapsedMillis` (already implied by start time, but let's make it explicit for preview/testing or just calculate in UI).
- [x] Actually, `HomeUiState` already has `startTime`. The UI can calculate elapsed.
- [x] Remove `progress` (float) from `HomeUiState` or deprecate it in favor of `elapsedMillis`.

## Phase 2: FluxTimer Component
- [x] Update `FluxTimer` signature to take `elapsedMillis` (or `duration`).
- [x] Implement `Canvas` logic:
    -   Loop through `elapsedHours / 24`.
    -   Draw full circles for completed days.
    -   Draw partial arc for current day.
    -   Adjust radius for each subsequent day (concentric).

## Phase 3: Integration
- [x] Update `HomeScreen` to pass data to new `FluxTimer`.

## Phase 4: Validation
- [x] Set start time to yesterday -> Verify full + partial ring.
