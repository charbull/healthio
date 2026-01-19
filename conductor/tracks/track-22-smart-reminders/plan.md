# Implementation Plan - Track 22: Smart Reminders

- [ ] **Manifest & Perms:**
    - [ ] Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` to `AndroidManifest.xml`.
    - [ ] Update `MainActivity.kt` to request permission on launch (simple approach).
- [ ] **DAO Updates:**
    - [ ] `MealDao`: Add `getCountBetween(start, end)`.
    - [ ] `WorkoutDao`: Add `getCountBetween(start, end)`.
- [ ] **Worker Implementation:**
    - [ ] Create `app/src/main/java/com/healthio/core/worker/ReminderWorker.kt`.
    - [ ] Implement `doWork`: switch on type, check DAO, send notification.
- [ ] **Scheduler Implementation:**
    - [ ] Create `app/src/main/java/com/healthio/core/worker/ReminderScheduler.kt`.
    - [ ] implement `scheduleAll(context)`: calculates delay for next 10am, 1pm, etc., and enqueues unique work.
- [ ] **Integration:**
    - [ ] Call `ReminderScheduler.scheduleAll(context)` in `HomeViewModel.init` or `MainActivity`.
