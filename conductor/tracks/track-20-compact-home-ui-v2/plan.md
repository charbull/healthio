# Implementation Plan - Track 20: Compact Home UI v2

- [ ] Modify `app/src/main/java/com/healthio/ui/dashboard/HomeScreen.kt`:
    - [ ] Locate `FastingSection` composable.
    - [ ] Remove the `Text` block that shows "FASTING TIME" or "READY".
    - [ ] Reduce `Spacer` height between `TimeDisplay` and `Button` (24dp -> 16dp).
    - [ ] Reduce Header bottom padding (16dp -> 8dp).
