# Glucose Tracker App — Phase 1 Implementation Plan
**Scope:** Manual entry + local storage + settings + overview stats.
**Target:** Android Studio project, Kotlin, Jetpack Compose, Room.

> [!NOTE]
> **Implementation Status:** Phase 1 core functionality is fully implemented. The originally planned Phase 2 OCR meter scanning has been **shelved for now** in favor of advanced analytics and clinical bolus/carb calculation tools. See [README.md](../README.md) and [docs/README.md](README.md) for the active documentation and roadmap.

---

## 0. Ground rules for the implementing agent

- Use **Kotlin + Jetpack Compose** for all UI. Use **Room** for persistence. Use **DataStore (Preferences)** for settings, not SharedPreferences directly.
- Target/compile SDK 30. Do not introduce APIs gated above API 30 (check `@RequiresApi` annotations on anything used from `androidx.*` or `java.time.*` — see note on `java.time` below).
- **java.time note:** `java.time` (LocalDate, LocalTime, Instant) requires API 26+ unless desugaring is enabled. Since minSdk is unspecified but ceiling is API 30, enable **Java 8+ API desugaring** in Gradle so `java.time` can be used safely even if minSdk ends up below 26. This is simpler than threeten-abp and is the modern recommended approach.
- Build one vertical slice at a time and get it compiling/running before moving to the next step. Order matters — later steps depend on earlier ones.
- Do not implement Phase 2 (camera/OCR) scaffolding yet beyond a single disabled/non-functional button placeholder on the Overview screen, per Step 7.
- All glucose values are stored internally in a **single canonical unit** (mg/dL) regardless of display unit, to make unit-switching in Settings non-destructive. Conversion happens only at the UI layer.

---

## 1. Project structure

Create the following package layout under the app module:

```
com.<package>.glucosetracker/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── GlucoseEntry.kt              (Room @Entity)
│   │   ├── GlucoseEntryDao.kt
│   │   └── Converters.kt                (Room TypeConverters for enums/Instant)
│   ├── settings/
│   │   ├── UserPreferences.kt           (data class: unit, lowThreshold, highThreshold)
│   │   └── SettingsRepository.kt        (DataStore wrapper)
│   └── repository/
│       └── GlucoseRepository.kt         (mediates DAO + mapping)
├── domain/
│   ├── model/
│   │   ├── GlucoseUnit.kt               (enum: MG_DL, MMOL_L)
│   │   ├── Period.kt                    (enum: FASTING, BEFORE_BREAKFAST, AFTER_BREAKFAST, BEFORE_LUNCH, AFTER_LUNCH, ... see Step 3)
│   │   ├── InsulinType.kt               (enum: NONE, LONG_ACTING, SHORT_ACTING, BOTH)
│   │   └── GlucoseStatus.kt             (enum: LOW, IN_RANGE, HIGH — derived, not stored)
│   └── util/
│       └── GlucoseUnitConverter.kt      (mg/dL <-> mmol/L conversion functions)
├── ui/
│   ├── navigation/
│   │   └── AppNavHost.kt
│   ├── overview/
│   │   ├── OverviewScreen.kt
│   │   └── OverviewViewModel.kt
│   ├── logbook/
│   │   ├── LogbookScreen.kt
│   │   └── LogbookViewModel.kt
│   ├── entry/
│   │   ├── EntryScreen.kt               (the "Edit Sugr Note" form)
│   │   └── EntryViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   ├── you/
│   │   └── YouScreen.kt                 (hosts entry point to Settings; minimal in phase 1)
│   └── theme/
│       └── (standard Compose theme files)
└── MainActivity.kt
```

Rationale: domain enums are kept separate from Room entities so the UI/business logic never depends directly on persistence annotations, and unit conversion is centralized in one util so it's never duplicated across screens.

---

## 2. Database layer

### 2.1 `GlucoseEntry` entity

Fields:
- `id: Long` — autogenerate primary key
- `glucoseMgDl: Float` — canonical stored value, always mg/dL regardless of display unit
- `dateTime: Long` — epoch millis (store as Long, not java.time object directly, for simplest Room compatibility)
- `period: Period` — enum, see Step 3
- `insulinType: InsulinType` — enum, default `NONE`
- `longActingUnits: Float?` — nullable, populated only if `insulinType` is `LONG_ACTING` or `BOTH`
- `shortActingUnits: Float?` — nullable, populated only if `insulinType` is `SHORT_ACTING` or `BOTH`
- `hasFood: Boolean` — default `false`
- `carbAmount: Float?` — nullable, populated only if `hasFood == true`
- `sourceType: EntrySource` — enum `MANUAL` or `IMAGE` (store now even though only `MANUAL` is used in Phase 1, so Phase 2 doesn't require a migration)
- `imagePath: String?` — nullable, unused in Phase 1, reserved for Phase 2

### 2.2 Enums (Room TypeConverters)

```kotlin
enum class Period {
    FASTING, BEFORE_BREAKFAST, AFTER_BREAKFAST,
    BEFORE_LUNCH, AFTER_LUNCH, RANDOM
}
```
Note: the wireframe shows 6 period options (Fasting, Random, Before Breakfast, After Breakfast, Before Lunch, After Lunch) with no "before/after dinner" — implement exactly these 6 for Phase 1. If the user wants dinner periods added later, that's a one-line enum change plus UI grid addition; flag this to the user but don't add it speculatively.

```kotlin
enum class InsulinType { NONE, LONG_ACTING, SHORT_ACTING, BOTH }
enum class EntrySource { MANUAL, IMAGE }
enum class GlucoseUnit { MG_DL, MMOL_L }
```

Use a single `Converters.kt` with `@TypeConverter` functions mapping each enum to/from `String` (its `.name`), rather than `Int` ordinals — this avoids silent data corruption if enum order ever changes.

### 2.3 DAO

```kotlin
@Dao
interface GlucoseEntryDao {
    @Insert
    suspend fun insert(entry: GlucoseEntry): Long

    @Update
    suspend fun update(entry: GlucoseEntry)

    @Delete
    suspend fun delete(entry: GlucoseEntry)

    @Query("SELECT * FROM glucose_entries ORDER BY dateTime DESC")
    fun observeAll(): Flow<List<GlucoseEntry>>

    @Query("SELECT * FROM glucose_entries WHERE id = :id")
    suspend fun getById(id: Long): GlucoseEntry?

    @Query("SELECT * FROM glucose_entries ORDER BY dateTime DESC LIMIT 1")
    fun observeLatest(): Flow<GlucoseEntry?>

    @Query("SELECT AVG(glucoseMgDl) FROM glucose_entries WHERE dateTime >= :sinceMillis")
    fun observeAverageSince(sinceMillis: Long): Flow<Float?>
}
```

### 2.4 `AppDatabase`

Standard Room singleton via `Room.databaseBuilder(context, AppDatabase::class.java, "glucose_tracker.db").build()`. Version = 1. Do not add `fallbackToDestructiveMigration()` — Phase 2 will need a real migration to add OCR-related columns (already reserved as nullable above, so no migration is even needed when Phase 2 lands).

---

## 3. Settings (DataStore)

### 3.1 Stored preferences

- `glucoseUnit: GlucoseUnit` — default `MG_DL`
- `lowThresholdMgDl: Float` — default `70f` (standard low threshold; store canonical in mg/dL same as entries)
- `highThresholdMgDl: Float` — default `180f` (standard postprandial high threshold; reasonable default, user-editable)

Use Proto DataStore or Preferences DataStore — **use Preferences DataStore** for Phase 1 simplicity (three scalar values don't justify a `.proto` schema).

### 3.2 `SettingsRepository`

Expose as `Flow<UserPreferences>` for reactive consumption, plus suspend setters:
```kotlin
suspend fun setGlucoseUnit(unit: GlucoseUnit)
suspend fun setLowThreshold(mgDl: Float)
suspend fun setHighThreshold(mgDl: Float)
val preferencesFlow: Flow<UserPreferences>
```

### 3.3 Settings screen behavior

- Unit picker: segmented control or radio group, "mg/dL" vs "mmol/L".
- Threshold inputs: numeric text fields. **Important UX detail:** the threshold input fields should display/accept values in the *currently selected unit*, but always convert to mg/dL before persisting. If the user switches units after setting thresholds, the displayed threshold values should auto-convert (e.g. 70 mg/dL → 3.9 mmol/L) rather than silently reinterpreting the stored number in the new unit.
- Validate `lowThreshold < highThreshold` before allowing save; show inline error otherwise.

---

## 4. Unit conversion utility

```kotlin
object GlucoseUnitConverter {
    fun mgDlToMmolL(mgDl: Float): Float = mgDl / 18.0182f
    fun mmolLToMgDl(mmolL: Float): Float = mmolL * 18.0182f

    fun format(mgDl: Float, unit: GlucoseUnit): String = when (unit) {
        GlucoseUnit.MG_DL -> mgDl.roundToInt().toString()
        GlucoseUnit.MMOL_L -> String.format("%.1f", mgDlToMmolL(mgDl))
    }
}
```
This is the **only** place conversion math lives. Every screen that displays or accepts a glucose value goes through this object — do not duplicate the conversion constant elsewhere.

---

## 5. Entry screen ("Edit Sugr Note" form)

This is the core of Phase 1. Build from the `Entry.png` wireframe exactly, with the following conditional logic made explicit:

### 5.1 Fields and layout (top to bottom)

1. **Date** — tap opens a `DatePickerDialog`. Default to current date for new entries.
2. **Time** — tap opens a `TimePickerDialog`. Default to current time for new entries.
3. **Period** — single-select grid of 6 chips/buttons (`Fasting`, `Random`, `Before Breakfast`, `After Breakfast`, `Before Lunch`, `After Lunch`). Default selection: `Random` (matches wireframe's blue default). Selecting one deselects any other (radio-button semantics, rendered as text buttons per the wireframe's visual style, not actual radio circles).
4. **Glucose value input** — *(not visible in the wireframe crop, but required since this is the actual logged data point)*. Add a numeric input field for the glucose value itself, labeled with the active unit from Settings (e.g. "Glucose (mg/dL)"). Flag this explicitly to the user: the wireframe didn't show this field but the form cannot be saved without it. Place it logically near the top, e.g. directly under Time, before Period.
5. **Insulin** — single-select: `Long Acting`, `Short Acting`, `Both`, `None`. Default: `None` (blue in wireframe).
   - If `Long Acting` or `Both` selected → reveal **"Long Acting units:"** numeric input.
   - If `Short Acting` or `Both` selected → reveal **"Short Acting units:"** numeric input.
   - If `None` selected → hide both unit inputs and clear their values.
6. **Food** — single-select: `Yes`, `No`. Default: `No` (blue in wireframe).
   - If `Yes` selected → reveal **"Carb amount"** numeric input.
   - If `No` selected → hide and clear it.
7. **Save** button — bottom right, primary color, per wireframe.

### 5.2 Conditional field visibility — implementation pattern

Use Compose's `AnimatedVisibility` for each conditional field so toggling doesn't feel abrupt:
```kotlin
AnimatedVisibility(visible = insulinType == InsulinType.LONG_ACTING || insulinType == InsulinType.BOTH) {
    OutlinedTextField(/* Long Acting units */)
}
```
When a field becomes hidden, **clear its backing state to null** at the same time (not just visually hide it) — this prevents stale data being saved if the user toggles Insulin to None after typing a units value.

### 5.3 Validation before Save

- Glucose value: required, must be a positive number, reasonable range check (e.g. reject < 20 or > 600 mg/dL equivalent with a confirmation dialog — "this seems like an unusual value, save anyway?" rather than a hard block, since edge cases do happen medically).
- Date/Time: required (defaulted, so effectively always present unless user clears it — guard anyway).
- Long/Short Acting units: required *only if* their section is visible; must be ≥ 0.
- Carb amount: required *only if* Food = Yes; must be ≥ 0.
- On validation failure: inline field-level error text, don't block on a dialog.

### 5.4 EntryViewModel responsibilities

- Holds all form field state (or delegates to a `data class EntryFormState`).
- Converts displayed glucose value (in user's selected unit) to mg/dL before constructing the `GlucoseEntry` for persistence.
- Exposes `saveEntry()` which validates, persists via repository, and emits a navigation event back to Logbook/Overview on success.
- Supports both **create** and **edit** modes — accept an optional `entryId: Long?` nav argument; if non-null, load the existing entry via `getById` and pre-populate the form (needed for editing existing logbook entries, which you'll want even in Phase 1).

---

## 6. Logbook screen

- List of all entries, `observeAll()` from DAO, newest first.
- Each row: date, time, glucose value (formatted in current unit via `GlucoseUnitConverter`), period tag, and a colored indicator (red/green/amber) if the value is outside the user's low/high thresholds (use `GlucoseStatus` derived enum — computed in the ViewModel, not stored).
- Tap a row → navigate to Entry screen in edit mode (pass `entryId`).
- Swipe-to-delete or long-press delete, your choice — swipe-to-delete is more idiomatic for a list like this; implement with `SwipeToDismissBox` (Compose Material 3) if available in your Compose BOM version, otherwise a simple delete icon button per row is an acceptable fallback for Phase 1.

---

## 7. Overview screen

Per your answer, build basic stats now:

- **Latest reading** card: value (formatted in current unit), date/time, period tag, and color-coded status (Low/In Range/High) against the user's thresholds.
- **Average** card: average glucose over a fixed recent window — use **last 7 days** for Phase 1 (simplest, most clinically standard short window; mention to the user that 14/30-day toggle is an easy Phase 1.5 addition if wanted).
- Below stats: the two buttons from `Android_Large_-_1.png` — **"add record from image"** and **"manual record"**, plus the floating **+** action button (make the FAB and "manual record" button do the same thing — both should navigate to the Entry screen in create mode, no `entryId`; having two affordances for the same action is fine per the wireframe, don't fight the existing design).
- **"add record from image"** — per ground rules, this button should be present but disabled (greyed out, e.g. `enabled = false` with a tooltip/toast "Coming soon") since OCR is Phase 2. Do not stub a fake camera flow.
- Bottom navigation bar: `Overview`, `Logbook`, `You` — implement via `NavigationBar` (Material 3) matching the wireframe icons (bar chart, list, person). `You` tab in Phase 1 just needs to contain a way to reach Settings (e.g. a single "Settings" list item) — no other profile features needed yet.

---

## 8. Navigation graph

Use Compose Navigation (`androidx.navigation:navigation-compose`). Routes:

```
"overview"
"logbook"
"you"
"settings"
"entry?entryId={entryId}"   // entryId nullable/optional arg
```

Bottom nav switches between `overview`, `logbook`, `you` as top-level destinations (use `popUpTo` + `saveState`/`restoreState` so tab state survives switching, standard pattern). `entry` and `settings` are pushed on top, not part of the bottom nav.

---

## 9. Build order (do these in sequence, verify compile + run after each)

1. Project skeleton: package structure, Compose theme, empty `MainActivity` with bottom nav scaffold and 3 empty placeholder screens. Verify navigation works before adding any data layer.
2. Domain enums (`Period`, `InsulinType`, `EntrySource`, `GlucoseUnit`) — pure Kotlin, no dependencies.
3. `GlucoseUnitConverter` + unit tests for it (simple input/output pairs, including round-trip conversion).
4. Room entity, DAO, TypeConverters, AppDatabase. Verify with a quick manual insert/query smoke test (e.g. a temporary button that inserts a dummy row and logs the result) before building real UI on top.
5. DataStore `SettingsRepository` + `UserPreferences`. Verify read/write round-trips.
6. Entry screen UI with all conditional logic from Step 5, wired to a real `EntryViewModel` and real persistence — this is the largest single piece, build field-by-field rather than all at once.
7. Logbook screen, wired to real data, including edit and delete.
8. Overview screen stats cards, wired to real data, plus navigation buttons/FAB.
9. Settings screen, wired to real `SettingsRepository`, with unit-aware threshold display/conversion.
10. Pass over all screens checking unit-display consistency (does every glucose number on every screen respect the currently selected unit?) and conditional-field-clearing correctness (Step 5.2).

---

## 10. Explicit Phase 1 exclusions (do not build yet)

- Camera capture, OpenCV, ML Kit, any OCR or image pipeline.
- Charts/graphs beyond the two stat cards.
- Data export/backup.
- Multi-user/profile support beyond a single local user.
- Reminder/notification scheduling.
- Dinner-related Period options (flagged in Step 2.2 as a possible future addition).

These are natural Phase 2+ candidates and should be raised with the user again once Phase 1 is stable, rather than speculatively scaffolded now.

---

## 11. Open items to confirm with the user before/while building

- Confirm the actual glucose-value input field placement and label wording (Step 5.1 item 4) — this was inferred since it wasn't visible in the wireframe crop.
- Confirm default low/high threshold values (70 / 180 mg/dL assumed — standard but should be user-confirmed, not just agent-assumed).
- Confirm whether `Period` needs Dinner options or 6 total is intentional/final.
