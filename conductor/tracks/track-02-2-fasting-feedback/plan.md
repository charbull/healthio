# Implementation Plan - Track 02.2

## Phase 1: Data
- [x] Create `com.healthio.core.data.QuotesRepository`.
- [x] Add list of quotes (Strings).

## Phase 2: Logic Updates
- [x] Update `HomeUiState` to include `showFeedbackDialog` (Boolean), `completedDuration` (String), and `feedbackQuote` (String).
- [x] Update `HomeViewModel`:
    -   `endFast()` should first set `showFeedbackDialog = true` and calculate duration.
    -   `dismissFeedback()` should actually call `repository.endFast()` and reset state.

## Phase 3: UI Implementation
- [x] Create `FastCompletedDialog` composable.
- [x] Integrate Dialog into `HomeScreen`.

## Phase 4: Validation
- [x] Run build.
- [x] Verify flow: Fasting -> End Fast -> Dialog -> Dismiss -> Ready.
