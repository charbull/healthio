# Track 03.3: Gallery Import

**Goal:** Allow users to select food images from their device gallery.

## Scope
1.  **UI:** Add "Gallery" button to `CameraContent`.
2.  **Logic:**
    -   Launch Photo Picker.
    -   Read URI -> Bitmap.
    -   Pass to `ReviewContent`.

## Success Criteria
-   Clicking "Gallery" opens the system picker.
-   Selected image appears in the Review screen.
-   Analysis works same as camera.
