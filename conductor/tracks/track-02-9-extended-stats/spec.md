# Track 02.9: Extended Stats Views

**Goal:** Add Monthly and Yearly aggregation views to the fasting history.

## Scope
1.  **ViewModel:**
    -   `timeRange` state: Week, Month, Year.
    -   `chartData` state: List of `ChartEntry` dynamically mapped.
2.  **Chart Component:**
    -   Rename `WeeklyFastingChart` to `FastingChart`.
    -   Support dynamic X-axis formatters (Day vs Month).
3.  **UI:**
    -   Add toggle controls to `StatsScreen`.

## Success Criteria
-   User can switch between Week, Month, Year.
-   Chart updates to show correct aggregation.
-   Month view shows daily bars.
-   Year view shows monthly average (or total?) bars. (Total hours is standard).
