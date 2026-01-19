# Implementation Plan - Track 10: Fix Month Chart UI

- [ ] Modify `HealthioChart.kt`:
    - [ ] Calculate `pointCount` from the first series.
    - [ ] Define `barThickness` and `barSpacing` logic:
        - [ ] If `pointCount > 20`: thickness 6.dp, spacing 4.dp.
        - [ ] Else: keep existing logic (12.dp / 4.dp based on series count).
    - [ ] Apply these values to `lineComponent` and `columnChart`.
