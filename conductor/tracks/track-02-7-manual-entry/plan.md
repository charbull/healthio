# Implementation Plan - Track 02.7

## Phase 1: Logic
- [x] Add `logPastFast(start, end)` to `HomeViewModel`.
- [x] Ensure it writes to `FastingRepository`.

## Phase 2: UI UX
- [x] Update `HomeScreen`:
    -   Change "Start from specific time..." to "Manual Entry".
    -   Show a Dialog: "Ongoing Fast" or "Completed Fast"?
    -   **Ongoing**: Trigger existing Date/Time picker flow -> `startFastAt`.
    -   **Completed**: Trigger Start Date/Time -> End Date/Time -> `logPastFast`.

## Phase 3: Integration
- [x] Handle End Date logic (End time might be next day).
- [x] Validation (End > Start).

## Phase 4: Verification
- [x] Test logging a fast from yesterday.
- [x] Check History Chart to see it appear.
