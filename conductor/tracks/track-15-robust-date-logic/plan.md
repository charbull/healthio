# Implementation Plan - Track 15: Robust Date Logic

- [ ] Modify `app/src/main/java/com/healthio/ui/stats/StatsUtils.kt`:
    - [ ] Import `java.time.temporal.TemporalAdjusters`.
    - [ ] Update `TimeRange.Week` logic to use `previousOrSame(DayOfWeek.MONDAY)`.
- [ ] Run `./gradlew test` to verify.
