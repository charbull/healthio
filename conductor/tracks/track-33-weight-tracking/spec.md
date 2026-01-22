# Track 33: Weight Tracking

**Goal:** Allow users to track their body weight, specifically by syncing with Withings smart scales via Health Connect.

## Scope
1.  **Data Layer:**
    -   New `WeightLog` entity (timestamp, value_kg, source).
    -   `WeightDao` for storage.
2.  **Health Connect Integration:**
    -   Add `READ_WEIGHT` permission.
    -   Fetch `WeightRecord`.
3.  **UI:**
    -   Display current weight on Home Screen or Stats.
    -   Add "Sync Weight" capability (likely grouped with Health Connect Sync).

## Success Criteria
-   App requests Weight permissions from Health Connect.
-   App fetches weight data (from Withings via Health Connect).
-   Latest weight is displayed in the app.
