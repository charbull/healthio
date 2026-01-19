# Implementation Plan - Track 21: Timer Inside Circle

- [ ] Modify `app/src/main/java/com/healthio/ui/components/FluxTimer.kt`:
    - [ ] Update signature to accept `timeDisplay: String`.
    - [ ] Wrap `Canvas` in a `Box`.
    - [ ] Add `Text` inside the `Box`, aligned to `Alignment.Center`.
- [ ] Modify `app/src/main/java/com/healthio/ui/dashboard/HomeScreen.kt`:
    - [ ] Update `FluxTimer` call to pass `uiState.timeDisplay`.
    - [ ] Remove the standalone `Text(text = uiState.timeDisplay, ...)` block.
