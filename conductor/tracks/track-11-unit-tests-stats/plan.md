# Implementation Plan - Track 11: Unit Tests for Stats Logic

## Phase 1: Refactor
- [ ] Create `app/src/main/java/com/healthio/ui/stats/StatsUtils.kt`.
- [ ] Move `getBucketIndex` logic into `StatsUtils` object.
- [ ] Update `StatsViewModel.kt` to use `StatsUtils.getBucketIndex`.

## Phase 2: Create Tests
- [ ] Create `app/src/test/java/com/healthio/ui/stats/StatsUtilsTest.kt`.
- [ ] Write tests for `TimeRange.Week`.
- [ ] Write tests for `TimeRange.Month` (covering the fix).
- [ ] Write tests for `TimeRange.Year`.
- [ ] Run tests using `./gradlew test`.
