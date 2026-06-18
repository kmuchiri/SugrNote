# Phase 1 Improvements

While the core functionality of Phase 1 is implemented, there are several areas where the codebase and user experience could be improved before moving to Phase 2:

## Technical Improvements
- **Testing**: Add more comprehensive unit tests for ViewModels (`EntryViewModel`, `OverviewViewModel`, `LogbookViewModel`) and the Repository layer. Add UI tests for Compose screens.
- **UI State Modeling**: Refactor ViewModels to use a single unified UI State data class exposed as a `StateFlow` instead of having multiple separate state variables, minimizing race conditions in state updates.
- **Error Handling**: Implement more robust error handling and user feedback (e.g., Snackbars) when database or settings operations fail.
- **Database Migrations**: Prepare robust testing for Room migrations ahead of Phase 2, ensuring that adding OCR specific fields or new tables doesn't crash existing users.

## User Experience (UX) Enhancements
- **Swipe-to-delete Undo**: Ensure swipe-to-delete in the Logbook has an "Undo" action via Snackbar to prevent accidental data loss.
- **Form Validation**: Improve the validation feedback on the `EntryScreen` by showing inline error messages as the user types, rather than only upon clicking "Save".
- **Dynamic Stats Window**: Add a toggle on the Overview screen to switch the average window between 7 days, 14 days, and 30 days.
- **Empty States**: Improve empty state illustrations and copy on the Logbook and Overview screens when no entries exist to better guide new users.
- **Accessibility**: Ensure all buttons, FABs, and text fields have proper `contentDescription` and semantic properties for screen readers.
