# Implementation Plan - Track 17: Compact Home Screen UI

- [ ] Modify `app/src/main/java/com/healthio/ui/dashboard/HomeScreen.kt`:
    - [ ] **FastingSection:**
        - [ ] Remove the `Text(text = ... "FASTING TIME" / "READY" ...)` composable.
        - [ ] Reduce `Spacer` height between Timer and TimeDisplay (32dp -> 16dp).
        - [ ] Reduce `Spacer` height between TimeDisplay and Button (32dp -> 24dp).
        - [ ] Reduce Button height (56dp -> 50dp).
    - [ ] **Main Column:**
        - [ ] Reduce Header bottom padding (32dp -> 16dp).
        - [ ] Reduce Divider spacing (48dp -> 24dp).
    - [ ] **EnergySection:**
        - [ ] Reduce `Card` internal padding (20dp -> 16dp).
