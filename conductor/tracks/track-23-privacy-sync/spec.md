# Track 23: Privacy-Focused Sync (Drive.File Scope)

**Goal:** Restrict Google Drive access to the "Least Privilege" model using the `drive.file` scope and provide clear privacy communication to the user.

## Scope
1.  **Scope Restriction:**
    -   Audit and update the Google Sign-In request to strictly use `https://www.googleapis.com/auth/drive.file`.
    -   Remove broader scopes like `https://www.googleapis.com/auth/spreadsheets` if they grant access to files not created by Healthio.
2.  **Privacy Communication (UX):**
    -   Before the Google Sign-In trigger, display an information card or dialog explaining the privacy benefits of the `drive.file` scope.
    -   "Healthio only accesses the specific data spreadsheet it creates. Your other Drive files remain completely private and invisible to us."
3.  **Verification:**
    -   Ensure the app can still create, find, and append to its specific "Healthio Dashboard Data" spreadsheet with the restricted scope.

## Success Criteria
-   The Google Consent screen explicitly mentions "View and manage Google Drive files and folders that you have opened or created with this app."
-   The user is informed about this isolation before they click "Sign In".
