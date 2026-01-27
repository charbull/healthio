# Implementation Plan - Track 35: Meal History Grouping

- [x] **Logic:**
    - [x] Create a helper function or ViewModel logic to group `List<MealLog>` into `Map<LocalDate, List<MealLog>>` or a list of sealed class items (Header/Item).
- [x] **UI (`StatsScreen.kt`):**
    - [x] Refactor `MealHistoryList` to accept grouped data or perform grouping internally.
    - [x] Implement a `MealHeader` composable.
    - [x] Update the list rendering to show Headers + Meal Items.
- [x] **Verification:**
    - [x] Verify "Today" and "Yesterday" labels appear correctly.
    - [x] Verify meals are correctly listed under their respective days.
