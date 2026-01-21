# Implementation Plan - Track 29: Debug Universal Stats & X-Axis

- [x] **Debug Week/Year Logic:**
    - [x] Create a temporary debug test `StatsDebug.kt` that prints out the exact mapping of `log.timestamp` -> `bucketIndex` -> `entryOf` X-value to console (stdout).
    - [x] Run this test to see if the indices match the labels.
- [x] **Fix X-Axis (Month):**
    - [x] Check `HealthioChart` axis formatter logic.
    - [x] Ensure `labels` list passed to chart is not empty or misaligned.
- [x] **Apply Fixes:**
    - [x] Modify `StatsViewModel.kt` based on debug findings.
    - [x] Modify `HealthioChart.kt` if necessary.
