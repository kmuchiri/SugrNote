# SugrNote Documentation Hub

Welcome to the comprehensive technical and operational documentation for **SugrNote** — an offline-first Android application designed for blood glucose tracking, insulin administration management, carbohydrate logging, and diabetes analytics.

---

## Table of Contents
1. [Architecture Overview](#1-architecture-overview)
2. [Feature Specifications & User Flows](#2-feature-specifications--user-flows)
   - [Glucose Logging & Editing](#glucose-logging--editing)
   - [Insulin & Dose Tracking](#insulin--dose-tracking)
   - [Nutrition & Carbohydrate Tracking](#nutrition--carbohydrate-tracking)
   - [Physical Activity & Exercise Tracking](#physical-activity--exercise-tracking)
   - [Analytics, Trends & Time-in-Range](#analytics-trends--time-in-range)
   - [Dual-Tab Logbook](#dual-tab-logbook)
   - [Android System Integrations (Notification & Widget)](#android-system-integrations)
   - [Settings, Thresholds & Personalization](#settings-thresholds--personalization)
3. [Data Architecture & Storage](#3-data-architecture--storage)
   - [Room Database Schema](#room-database-schema)
   - [DataStore Preferences](#datastore-preferences)
   - [Canonical Unit Conversion](#canonical-unit-conversion)
4. [Status of OCR (Optical Character Recognition)](#4-status-of-ocr-optical-character-recognition)
5. [Roadmap & Upcoming Features (Carb-to-Insulin Calculator)](#5-roadmap--upcoming-features)
6. [Developer & Build Guide](#6-developer--build-guide)

---

## 1. Architecture Overview

SugrNote is engineered following **Google's Modern Android Architecture** best practices, combining **Jetpack Compose** for a 100% declarative UI with a robust **MVVM** pattern and **Unidirectional Data Flow (UDF)**.

```
                    ┌─────────────────────────┐
                    │    UI Layer (Compose)   │
                    │ Screens & Custom Canvas │
                    └───────────┬─────────────┘
                                │ State / Events
                                ▼
                    ┌─────────────────────────┐
                    │       ViewModels        │
                    │   StateFlow & Coroutines│
                    └───────────┬─────────────┘
                                │ Reactive Streams (Flow)
                                ▼
                    ┌─────────────────────────┐
                    │    Repository Layer     │
                    │ Glucose & Settings Repos│
                    └───────────┬─────────────┘
                                │
         ┌──────────────────────┴──────────────────────┐
         ▼                                             ▼
┌─────────────────────────┐               ┌─────────────────────────┐
│ Room Database (SQLite)  │               │ Jetpack DataStore Prefs │
│ Entries, Injections,    │               │ Units, Thresholds,      │
│ Food, Exercise, Notes   │               │ Theme, Date/Time Formats│
└─────────────────────────┘               └─────────────────────────┘
```

### Core Architecture Principles
- **Offline-First & Local-Only**: All user entries, historical data, and configurations are stored in an encrypted/private on-device SQLite database. Zero external network calls are required for core functionality.
- **Unidirectional Data Flow (UDF)**: ViewModels expose immutable `StateFlow`s consumed by Compose views via `collectAsState()`. User interactions dispatch explicit intent methods to ViewModels.
- **Single Source of Truth**: Room queries return Kotlin `Flow`s. Any mutation in the database automatically triggers reactive updates to the UI, home screen widget, and system notification simultaneously.

---

## 2. Feature Specifications & User Flows

### Glucose Logging & Editing
- **Navigation**: Accessed via the persistent Floating Action Button (`+`) on Overview and Logbook, tapping any entry in the Logbook (edit mode), or tapping the home screen widget.
- **Input & Formatting**:
  - Accepts decimal values for mmol/L or integers for mg/dL according to user preferences.
  - Automatically converted to canonical mg/dL for database persistence.
- **Safety Checks**:
  - Enforces positive glucose values.
  - Unusual range guard: Readings `< 20 mg/dL` or `> 600 mg/dL` trigger an explicit confirmation dialog (`showUnusualValueDialog`) to catch keystroke errors without restricting genuine medical extremities.
- **Period & Meal Categorization**:
  - `FASTING`
  - `BEFORE_MEAL` / `AFTER_MEAL` with meal subtypes: `BREAKFAST`, `LUNCH`, `DINNER`, `SNACK`
  - `BEFORE_SLEEP`
  - `RANDOM`

### Insulin & Dose Tracking
- **Types Supported**: `NONE`, `LONG_ACTING` (Basal), `SHORT_ACTING` (Bolus), or `BOTH`.
- **Dynamic Field Visibility**: Insulin unit input fields animate smoothly into view only when the relevant insulin type is chosen. Toggling back to `NONE` resets and clears units.
- **Real-Time Aggregations**:
  - **24-Hour Card**: Sums total units taken in the last 24 hours, count of long-acting injections, count of short-acting injections, and displays timestamps for the most recent doses.
  - **Historical Average Card**: Calculates average daily long-acting, short-acting, and total insulin across 7, 14, 30, 60, and 90-day intervals.

### Nutrition & Carbohydrate Tracking
- **Grams of Carbs**: Numeric input field revealed when `hasFood` is true.
- **Auto-Detection**: Selecting `BEFORE_MEAL` automatically activates food logging.
- **Logbook Visibility**: Entries with food display a restaurant badge in the Glucose view, and explicit carb counts in the Dose view.

### Physical Activity & Exercise Tracking
- **Timing Options**: `NONE`, `BEFORE_EXERCISE`, `AFTER_EXERCISE`.
- **Duration & Intensity**:
  - When `BEFORE_EXERCISE` is selected, fields for exercise duration (in minutes) and intensity (`Light`, `Moderate`, `Vigorous`) become accessible.
- **Contextual Visualization**: Correlated exercise details appear as tags in the Logbook and on the Latest Reading card.

### Analytics, Trends & Time-in-Range
- **Custom Canvas Trends Graph**:
  - Displays glycemic trajectories with horizontal threshold reference lines for individual Low and High limits.
  - **24-Hour Filter**: Sequential raw data point representation.
  - **Aggregated Filters (7D, 14D, 30D, 60D, 90D)**: Bins readings into 24 one-hour blocks (00:00 to 23:00) using nearest-half-hour rounding, connecting continuous averages. Single data point buckets are subtly rendered in neutral tones.
- **Time-in-Range (TIR) Breakdown**:
  - Calculates percentage and count of readings below low threshold, within target range, and above high threshold.
  - Renders a proportional segmented horizontal bar (`StatusLow`, `StatusInRange`, `StatusHigh`).
- **Mealtime Performance**:
  - Computes separate averages for Fasting, Before Meal, After Meal, Bedtime, and Random for any selected timeframe (7D through 90D).

### Dual-Tab Logbook
- **Tab 1: Glucose View**:
  - Chronological grouped date headers.
  - Color-coded circular status indicator.
  - Compound metadata chips (e.g. `Before Lunch · After Exercise`).
  - Quick action to delete with confirmation dialog.
  - Tap card to edit.
- **Tab 2: Dose View**:
  - Filtered exclusively for entries where insulin was administered.
  - Grouped by date with a daily header displaying cumulative units (e.g. `Total Dose: 32u`).
  - Distinct purple pill for Long-Acting units and blue pill for Short-Acting units, alongside associated carbs.

### Android System Integrations
- **Dynamic Persistent Notification (`GlucoseNotificationManager`)**:
  - Posts an ongoing, low-importance notification to the notification shade and lockscreen.
  - Displays latest reading, timestamp, and status.
  - Dynamically colored (`NotificationCompat.setColorized(true)`):
    - 🟢 Green (`#43A047`) for In-Range
    - 🔴 Red (`#E53935`) for Low
    - 🟠 Amber (`#FF8F00`) for High
  - Tap intent resumes `MainActivity`.
- **Home Screen App Widget (`AddEntryWidgetProvider`)**:
  - Displays latest reading, unit, status tint, and exercise notes.
  - Direct shortcut: Tapping anywhere on the widget triggers `ACTION_ADD_ENTRY`, opening the Add Entry screen instantly.
  - Synchronized via reactive Flow emissions in `MainActivity` as well as explicit broadcast triggers upon entry creation/deletion.

### Settings, Thresholds & Personalization
- **Glucose Unit**: Switch between `mg/dL` and `mmol/L`.
- **Target Thresholds**:
  - Custom Low and High boundaries.
  - Always presented and entered in the user's active unit, while validated and persisted in canonical `mg/dL`.
- **Time & Date Preferences**:
  - 12-Hour (AM/PM) or 24-Hour display.
  - `dd MMM, yyyy` (e.g., 22 Jun, 2026) or `MMM dd, yyyy` (e.g., Jun 22, 2026).
- **Themes**:
  - System default, Light, Dark, or OLED pure black (`#000000`).

---

## 3. Data Architecture & Storage

### Room Database Schema
Database name: `glucose_tracker.db` (Version 1).

**Table: `glucose_entries`**
| Column | Type | Nullable | Description |
|---|---|---|---|
| `id` | `INTEGER` | No | Primary Key, Auto-generate |
| `glucoseMgDl` | `REAL` | No | Canonical blood glucose in mg/dL |
| `dateTime` | `INTEGER` | No | Epoch milliseconds of reading |
| `period` | `TEXT` | No | Enum name (`FASTING`, `BEFORE_BREAKFAST`, etc.) |
| `exerciseTiming` | `TEXT` | No | Enum name (`NONE`, `BEFORE_EXERCISE`, `AFTER_EXERCISE`) |
| `insulinType` | `TEXT` | No | Enum name (`NONE`, `LONG_ACTING`, `SHORT_ACTING`, `BOTH`) |
| `longActingUnits`| `REAL` | Yes | Units of long-acting basal insulin |
| `shortActingUnits`| `REAL` | Yes | Units of short-acting bolus insulin |
| `hasFood` | `INTEGER` | No | Boolean flag indicating meal intake |
| `carbAmount` | `REAL` | Yes | Carbohydrates consumed in grams |
| `exerciseIntensity`| `TEXT` | Yes | `Light`, `Moderate`, `Vigorous` |
| `exerciseDuration` | `INTEGER` | Yes | Workout duration in minutes |
| `sourceType` | `TEXT` | No | `MANUAL` or `IMAGE` (reserved for future) |
| `imagePath` | `TEXT` | Yes | Optional path to reference image |
| `notes` | `TEXT` | No | User notes (max 254 characters) |

### DataStore Preferences
Preferences are managed in `user_preferences.preferences_pb`:
- `KEY_GLUCOSE_UNIT`: String (`MG_DL`, `MMOL_L`)
- `KEY_LOW_THRESHOLD`: Float (stored in mg/dL)
- `KEY_HIGH_THRESHOLD`: Float (stored in mg/dL)
- `KEY_THEME_MODE`: String (`SYSTEM`, `LIGHT`, `DARK`)
- `KEY_DARK_THEME_STYLE`: String (`STANDARD`, `OLED`)
- `KEY_IS_24_HOUR`: Boolean
- `KEY_DATE_FORMAT`: String (`DD_MMM_YYYY`, `MMM_DD_YYYY`)

### Canonical Unit Conversion
Conversion logic is strictly isolated in `GlucoseUnitConverter.kt`:
$$\text{mmol/L} = \frac{\text{mg/dL}}{18.0182}$$
$$\text{mg/dL} = \text{mmol/L} \times 18.0182$$

---

## 4. Status of OCR (Optical Character Recognition)

> [!IMPORTANT]
> **Optical Character Recognition (OCR) has been shelved for now.**

### Background & Evaluation
The original design concepts for SugrNote proposed camera capture and OCR to scan blood glucose meters. During development and testing, several practical challenges emerged:
1. **Hardware Variability**: Physical glucometers utilize divergent seven-segment LCD layouts, inversion polarizers, varying font weights, and differing decimal point placements.
2. **Lighting & Reflection**: Reflections on curved plastic screens often obscured segments, leading to misreadings.
3. **Friction vs. Convenience**: Taking a photo, cropping, and verifying an OCR output proved significantly slower and less reliable than the streamlined 3-second manual keypad entry.
4. **Clinical Priority**: Users and testers overwhelmingly prioritized deeper insulin logging, carbohydrate tracking, bolus assistance, and offline trend charts over camera scanning.

As a result, OCR development is paused. The underlying database schema retains `sourceType` and `imagePath` columns to allow seamless re-introduction in the future without database migrations if high-accuracy on-device vision models become available.

---

## 5. Roadmap & Upcoming Features

The complete roadmap, including detailed mathematical formulas, setting specifications, clinical report formats, and reminder architectures, is maintained in **[roadmap.md](roadmap.md)**.

### Summary of Roadmap Priorities
- **[Carb-to-Insulin Ratio (CIR) Calculator & Bolus Advisor](roadmap.md#1-priority-1-carb-to-insulin-ratio-cir-calculator--bolus-advisor)**: Dedicated wizard and entry form bolus calculator calculating meal and correction boluses with customizable time-of-day ratios, ISF, and IOB decay.
- **[Medical Data Export & Clinical AGP Reports](roadmap.md#priority-2-medical-data-export--clinical-reports)**: Formatted PDF reports for endocrinology visits and full CSV/JSON database exports.
- **[Smart Contextual Reminders](roadmap.md#priority-3-smart-contextual-reminders)**: Post-prandial 2-hour blood glucose test alerts and daily basal insulin injection alarms.
- **[Encrypted Local & Cloud Backups](roadmap.md#priority-4-encrypted-local--cloud-backups)**: Encrypted archive exports and optional user-owned cloud backups.

---

## 6. Developer & Build Guide

### Build Requirements
- Android Studio Ladybug (2024.2+) or later
- Android SDK 36 (targetSdk 36, compileSdk 36, minSdk 30)
- Java 17 / Kotlin 2.0+

### Key Commands
```bash
# Clean project
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Lint check
./gradlew lint
```
