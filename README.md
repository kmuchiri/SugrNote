# SugrNote

> **Modern, offline-first Android companion for logging, tracking, and understanding your blood glucose, insulin doses, carbs, and daily trends.**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Database-Room%20SQLite-FFCA28?logo=sqlite&logoColor=black)](https://developer.android.com/training/data-storage/room)
[![DataStore](https://img.shields.io/badge/Preferences-Jetpack%20DataStore-0F9D58)](https://developer.android.com/topic/libraries/architecture/datastore)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

## Features & Capabilities

SugrNote provides a complete, distraction-free diabetes management suite designed for individuals managing Type 1, Type 2, or gestational diabetes.

### 🩸 1. Comprehensive Glucose Logging

- **Fast, Flexible Manual Entry**: Log blood glucose readings with date, time, and custom tags in seconds.

- **Contextual Meal & Time Periods**:
  - `Fasting`
  - `Before Meal` / `After Meal` (paired with `Breakfast`, `Lunch`, `Dinner`, `Snack`)
  - `Before Sleep`
  - `Random`
- **Dual Unit Support (`mg/dL` & `mmol/L`)**:
  - Seamlessly switch units anytime in Settings.
  - All readings are stored canonically in `mg/dL` internally, preventing precision loss or corruption during conversions.

- **Unusual Value Safeguards**: Prompts a confirmation dialog if an entered reading is atypically low (< 20 mg/dL) or high (> 600 mg/dL) to prevent accidental typos while preserving medically valid edge cases.

- **Notes & Journaling**: Attach notes (up to 254 characters) to any entry for symptoms, mood, or context.

### 💉 2. Insulin & Dose Tracking

- **Multi-Insulin Regimen**: Log **Long-Acting** (basal), **Short-Acting** (bolus), **Both**, or **None**.
- **Precise Unit Measurement**: Input exact insulin units with decimal support.
- **24-Hour Insulin Summary**: Real-time aggregation of total units taken in the last 24 hours, long vs. short breakdown, and total injection counts.
- **Multi-Period Insulin Averages**: Calculate average daily doses and injection counts across 7D, 14D, 30D, 60D, and 90D windows.
- **Dedicated Dose Tab in Logbook**: View an organized chronological timeline specifically filtered for insulin doses with daily total summaries.

### 🥗 3. Food & Carbohydrate Tracking

- **Carb Logging (grams)**: Track carbohydrate consumption linked directly to meals and bolus insulin doses.
- **MealPeriod Coupling**: Selecting meal periods (`Before Meal`, `After Meal`) automatically prompts for carb intake.

### 🏃 4. Exercise & Physical Activity Correlation

- **Exercise Timing Correlation**: Tag readings as `None`, `Before Exercise`, or `After Exercise` to observe how physical exertion affects glycemic response.
- **Duration & Intensity**: Record workout duration (minutes) and intensity (`Light`, `Moderate`, `Vigorous`).
- **Activity Badges**: Activity information is clearly badged in both the Logbook and Overview cards.

### 📊 5. Visual Analytics, Trends & Time-in-Range (TIR)

- **Interactive Custom Canvas Trend Graph**:
  - **24-Hour View**: Plots actual raw readings sequentially across the day.
  - **Aggregated Hourly Trend (7D / 14D / 30D / 60D / 90D)**: Buckets readings into 24 hour-of-day slots, displaying mean trajectories and single-point representations.
- **Summary Metrics**:
  - Period Average, Total Reading Count, Lowest Reading, and Highest Reading.
  - **Time-in-Range (TIR) Bar**: Visual tri-color distribution bar showing proportion of readings that are **Low** (Red), **Target / In-Range** (Green), and **High** (Amber).
- **Averages by Meal Card**: Segmented breakdown of average glucose during Fasting, Before Meal, After Meal, Before Sleep, and Random across 7D, 14D, 30D, 60D, and 90D.
- **Latest Reading Card**: Glanceable hero card displaying the latest blood glucose level, timestamp, period, exercise timing, and range status.

### 📖 6. Dual-Mode Logbook

- **Glucose Tab**:
  - Grouped chronologically by date.
  - Colored indicator dots (`StatusLow`, `StatusInRange`, `StatusHigh`).
  - Icons indicating insulin administered (`Medication`) and food logged (`Restaurant`).
  - Period and exercise timing chips.
  - Tap to edit any entry; delete button with confirmation dialog.
- **Dose Tab**:
  - Grouped by date with a daily total dose header (e.g., `Total Dose: 28u`).
  - Distinct colored badges for Long-Acting (Purple) and Short-Acting (Blue) doses, plus logged carbs.

### 🔔 7. Persistent Android Notification

- **Live Lockscreen & Shade Monitor**: Always-visible low-importance notification displaying the most recent reading, timestamp, and status.
- **Dynamic Color-Coding**: Colored background tint that reflects glycemic status:
  - 🟢 **Green**: In Range
  - 🔴 **Red**: Low / Hypoglycemia
  - 🟠 **Amber**: High / Hyperglycemia
- **One-Tap Access**: Tapping the notification launches directly into the app.

### 📱 8. Homescreen App Widget

- **Glanceable Widget (`AddEntryWidgetProvider`)**: Place a widget on your home screen displaying your latest reading, unit, status color, time, and exercise timing.
- **Quick-Add Shortcut**: Tap the widget anywhere to jump straight into the Add Entry form.
- **Reactive Sync**: Automatically updates the instant an entry is added, updated, or removed in the app.

### 🎨 9. Customization & System Preferences

- **Theme Options**:
  - System default, Light mode, or Dark mode.
  - **Pure OLED Dark Mode**: True black (`#000000`) surfaces for OLED displays and battery savings.
- **Customizable Glycemic Thresholds**:
  - Set personal **Low Threshold** (default: 70 mg/dL / 3.9 mmol/L) and **High Threshold** (default: 180 mg/dL / 10.0 mmol/L).
  - Validation guarantees low threshold remains below high threshold.
- **Date & Time Formatting**:
  - 12-Hour (AM/PM) or 24-Hour military time.
  - Date formats: `dd MMM, yyyy` (e.g., *22 Jun, 2026*) or `MMM dd, yyyy` (e.g., *Jun 22, 2026*).

### 🔒 10. Privacy & Offline-First Architecture

- **100% On-Device Storage**: No mandatory accounts, logins, telemetry, or remote servers. All medical data stays strictly on your device in a local SQLite database.

---

## Application Structure

```

SugrNote
├── Overview Screen
│   ├── Latest Reading Card (Hero BG reading + Status + Exercise info)
│   ├── 24-Hour Insulin Intake Card (Long vs Short units + Timestamps)
│   ├── Trends & Averages Card (Custom Canvas chart + TIR bar + Stats + Range filters)
│   ├── Averages by Meal Card (Fasting, Before/After meal, Bedtime, Random)
│   └── Average Total Insulin Card (Long, Short, Total daily doses across 7–90D)
│
├── Logbook Screen
│   ├── Glucose View (Grouped by date, status pills, edit/delete actions)
│   └── Dose View (Grouped by date, total daily units, long/short/carbs)
│
├── Entry Screen (Add / Edit)
│   ├── Date & Time Pickers
│   ├── Glucose Value Input (with unit indication & range safety prompt)
│   ├── Period Selector (Fasting, Before/After Meal, Bedtime, Random)
│   ├── Meal Type (Breakfast, Lunch, Dinner, Snack)
│   ├── Insulin Selector & Unit Inputs (Long-Acting, Short-Acting, Both, None)
│   ├── Food & Carb Input (Grams of Carbohydrates)
│   ├── Exercise Timing, Duration & Intensity
│   └── Notes Input (up to 254 chars)
│
└── You / Settings Screen
    ├── Glucose Unit (mg/dL vs mmol/L)
    ├── Target Thresholds (Low & High)
    ├── Time & Date Formats (12h/24h, custom date patterns)
    └── Theme Options (System / Light / Dark / OLED)

```

## Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Architecture**: Clean Architecture + MVVM (Model-View-ViewModel) + Reactive UDF (Unidirectional Data Flow)
- **Local Database**: Room 2.6+ with SQLite, TypeConverters, and reactive Kotlin `Flow`s
- **Preferences Storage**: Jetpack DataStore (Preferences)
- **Asynchronous Execution**: Kotlin Coroutines & StateFlow
- **Date / Time Handling**: `java.time` (Desugared for backwards compatibility)
- **Hardware & System**: Android AppWidgetProvider, NotificationManager (Ongoing colorized notifications)
- **Compilation Target**:
  - `minSdk`: 30 (Android 11)
  - `targetSdk`: 36
  - `compileSdk`: 36
  - `Java/JVM`: 17

---

## Getting Started & Building

### Prerequisites

- Android Studio Ladybug (2024.2+) or newer
- Android SDK 36
- JDK 17

### Build & Run

1. Clone the repository:

   ```bash
   git clone https://github.com/kmuchiri/SugrNote.git
   cd SugrNote/app
   ```

2. Build debug APK:

   ```bash
   ./gradlew assembleDebug
   ```

3. Run unit tests:

   ```bash
   ./gradlew test
   ```

4. Install on a connected Android device or emulator:

   ```bash
   ./gradlew installDebug
   ```

---

## 📄 License

This project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
