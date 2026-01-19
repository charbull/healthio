# Track 16: ViewModel Aggregation Tests

**Goal:** Create pure unit tests to verify the aggregation logic for Energy and Nutrition stats, ensuring that daily sums are calculated correctly for Week, Month, and Year views.

## Scope
1.  **Test Coverage:**
    -   **Energy:** Verify `filterMealsIn` and `filterWorkoutsIn` correctly sum calories per bucket.
    -   **Nutrition:** Verify `filterMealsIn` correctly sums Protein, Carbs, and Fat per bucket.
2.  **Implementation:**
    -   Create `StatsAggregationTest.kt`.
    -   Re-implement the logic helpers (copied from ViewModel) to verify the algorithm itself in isolation.

## Success Criteria
-   Tests pass for Energy Intake, Energy Burned, and Macro aggregation.
