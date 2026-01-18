# Track 05.1: Nutrition Spreadsheet Sync

**Goal:** Sync meal logs (calories/macros) to Google Drive spreadsheet when charging and on Wi-Fi.

## Scope
1.  **Data Layer:**
    -   Update `MealLog` entity with `isSynced: Boolean`.
    -   Update `MealDao` with `getUnsyncedMeals()` and `markAsSynced()`.
2.  **Sync Logic (`BackupWorker`):**
    -   Fetch unsynced meals.
    -   Ensure a tab named `[Year]_Meals` exists.
    -   Append meal rows: `[Date, Time, Food Name, Calories, Protein, Carbs, Fat]`.
    -   Mark as synced in local DB.

## Success Criteria
-   Meal data appears in the same "Healthio Fasting Logs" spreadsheet in a separate tab.
-   Sync respects charging and Wi-Fi constraints.
