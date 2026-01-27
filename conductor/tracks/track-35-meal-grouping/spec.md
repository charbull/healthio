# Specification - Track 35: Meal History Grouping

## Goal
Improve the "Recent Meals" list in the Stats/Intake view by grouping meals by their date. Instead of a flat list, show a header for each day (e.g., "Today", "Yesterday", "Mon, Jan 22") followed by the meals for that day.

## Requirements
- **Grouping:** Group `recentMeals` by date (LocalDate).
- **Headers:** Display a sticky or distinct header for each group.
    - "Today" for current date.
    - "Yesterday" for previous date.
    - "DayOfWeek, MMM dd" for older dates.
- **Sorting:** Ensure days are sorted descending (newest first), and meals within days are sorted descending by time.
- **UI:** Update `StatsScreen.kt` and specifically the `MealHistoryList` composable.

## User Feedback
"ok for the recent meals view, I want to have them by day, maybe add a day and then list them below"
