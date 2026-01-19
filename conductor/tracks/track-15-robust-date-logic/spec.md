# Track 15: Robust Date Logic

**Goal:** Harden the date calculation logic in `StatsUtils` to ensure stability across locales and edge cases, addressing reported issues with Week/Year stats.

## Scope
1.  **StatsUtils Refactor:**
    -   Use `TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)` for Week start calculation instead of manual subtraction.
    -   Use `TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)` for Week end.
    -   Ensure `Year` logic explicitly uses `.year` comparison.
2.  **Verification:**
    -   Run existing unit tests to confirm no regression.
    -   (Implicit) Fix potentially brittle manual date math.

## Success Criteria
-   `StatsUtils` uses standard Java Time API adjusters.
-   Unit tests pass.
