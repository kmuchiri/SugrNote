# Phase 1 Features Status

## Fully Implemented
- **Project Structure**: Navigation graph, Compose theme, bottom navigation setup.
- **Database Layer**: Room database, `GlucoseEntry` entity, `GlucoseEntryDao`, TypeConverters for enums.
- **Domain Models**: Enums (`Period`, `InsulinType`, `EntrySource`, `GlucoseUnit`, `GlucoseStatus`).
- **Settings**: Preferences DataStore integration via `SettingsRepository` and `UserPreferences`.
- **Unit Conversion**: `GlucoseUnitConverter` utility for switching between `mg/dL` and `mmol/L`.
- **Entry Screen**: Manual logging of glucose levels, date/time, period, insulin types, and food intake with dynamic form fields.
- **Logbook Screen**: List of all glucose entries sorted by newest first, with unit-aware formatting and color-coded status.
- **Overview Screen**: Stats cards showing the latest reading and average glucose over the last 7 days.
- **You Screen**: Entry point for Settings.
- **Settings Screen**: UI to select glucose unit and define low/high thresholds with automatic conversions.

## Partially Implemented / Stubbed
- **OCR / Image Integration**: Placeholder button exists on the Overview screen but is disabled, awaiting Phase 2.
- **Database Schema**: `EntrySource.IMAGE` and `imagePath` are reserved in the database but currently unused.

## Not Yet Implemented (Phase 2+)
- **Camera Capture**: OpenCV, ML Kit, and OCR pipeline for reading glucose from images.
- **Charts / Graphs**: Advanced visualizations beyond basic stat cards.
- **Data Export / Backup**: Exporting glucose logs to CSV or cloud backup.
- **Multi-user Support**: Profiles for multiple users.
- **Reminders**: Notification scheduling for checking glucose levels.
- **Additional Periods**: Dinner-related period options (e.g., Before Dinner, After Dinner).
