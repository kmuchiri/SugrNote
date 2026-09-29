# SugrNote Feature List & Roadmap

This document tracks completed features, shelved experiments, and planned enhancements for SugrNote.

---

## ✅ Implemented Features

### 🩸 Core Blood Glucose Tracking
- [x] **Fast Manual Logging**: Streamlined entry creation with date and time pickers.
- [x] **Contextual Time Periods**:
  - Fasting, Random, Before Sleep.
  - Meal-paired periods: Before/After Breakfast, Before/After Lunch, Before/After Dinner, Before/After Snack.
- [x] **Dual Unit Support**: Support for `mg/dL` and `mmol/L` with zero-precision-loss canonical `mg/dL` database persistence.
- [x] **Out-of-Range Guard**: Unusual value prompt (< 20 mg/dL or > 600 mg/dL) to prevent typos.
- [x] **Notes**: Freeform entry notes (up to 254 characters) for symptoms, illness, or food notes.

### 💉 Insulin & Medication
- [x] **Multi-Insulin Regimen**: Independent tracking for Long-Acting (basal) and Short-Acting (bolus) insulin units.
- [x] **24-Hour Intake Monitor**: Real-time aggregation of total units taken in the past 24 hours with injection counts.
- [x] **Historical Insulin Averages**: 7-day, 14-day, 30-day, 60-day, and 90-day daily dosage averages and injection totals.
- [x] **Dedicated Dose Logbook Tab**: Distinct daily logbook view showcasing insulin dosages, color-coded long vs short acting pills, and associated carb intake.

### 🥗 Nutrition & Exercise
- [x] **Carbohydrate Tracking**: Record carb amount (grams) associated with meal times.
- [x] **Exercise Timing & Metrics**: Log physical activity timing (`Before Exercise`, `After Exercise`), duration in minutes, and intensity (`Light`, `Moderate`, `Vigorous`).
- [x] **Integrated UI Badges**: Exercise and food icons shown directly on logbook cards and overview tiles.

### 📊 Analytics & Insights
- [x] **Latest Reading Card**: Real-time hero display with color-coded status badge and relative timestamp.
- [x] **Interactive Canvas Trend Graph**:
  - 24-hour raw trajectory view.
  - Aggregated 24-hour hourly bucketing across 7D, 14D, 30D, 60D, and 90D timeframes.
- [x] **Time-in-Range (TIR) Distribution**: Tri-color bar reflecting Low (Red), In-Range (Green), and High (Amber) percentages and counts.
- [x] **Mealtime Averages Card**: Breakdown across Fasting, Before Meal, After Meal, Bedtime, and Random.
- [x] **Insulin Period Stats Card**: Breakdown of long, short, and total daily insulin averages across selectable periods.

### 📱 Android System Integrations
- [x] **Ongoing System Notification**: Low-overhead persistent notification displaying latest reading and status, dynamically tinted to match glycemic status (Low, In Range, High).
- [x] **Home Screen App Widget**: Glanceable widget displaying the latest reading, status color, time, and exercise timing, with an instant tap-to-add action.

### ⚙️ Personalization & Architecture
- [x] **Custom Target Thresholds**: Configurable Low Threshold and High Threshold with validation.
- [x] **Flexible Date & Time Formats**: 12-hour / 24-hour clock and multiple date format patterns (`dd MMM, yyyy` vs `MMM dd, yyyy`).
- [x] **Theme Options**: System, Light, Dark, and true OLED Black.
- [x] **Offline-First & Private**: Local-only SQLite Room database with Jetpack DataStore preferences.

---

## ⏸️ Shelved Features

### 📷 Optical Character Recognition (OCR) / Meter Screen Scanning
- **Status:** **Shelved for now**
- **Original Concept:** Using device camera + ML Kit / OpenCV to automatically detect blood glucose numbers from physical glucometer displays.
- **Reason for Shelving:**
  - High variance across physical glucometer LCD screens (segment displays, backlighting, glare, varying fonts and decimals).
  - Manual entry was optimized to be nearly instantaneous, reducing the need for error-prone camera steps.
  - Focus shifted toward core clinical tools: carb tracking, insulin dose logging, trend analysis, and ratio calculators.
  - May be revisited in a future major release once on-device computer vision models for medical segment displays mature.

---

## 📋 To Be Implemented (Upcoming Roadmap)

*Detailed specifications and clinical formulas are documented in **[roadmap.md](roadmap.md)**.*

### 🧮 1. Carb-to-Insulin Ratio (CIR) Calculator & Bolus Advisor
- **Goal:** Provide an in-app calculator to assist users in calculating meal boluses and correction boluses accurately.
- **Key Capabilities:**
  - **Meal Bolus Calculation**:
    $$\text{Meal Bolus} = \frac{\text{Carbs (grams)}}{\text{Carb-to-Insulin Ratio (g/u)}}$$
  - **Correction Bolus (High Blood Sugar Correction)**:
    $$\text{Correction Bolus} = \frac{\text{Current Blood Glucose} - \text{Target Blood Glucose}}{\text{Insulin Sensitivity Factor (ISF / Correction Factor)}}$$
  - **Total Suggested Dose**:
    $$\text{Total Bolus} = \text{Meal Bolus} + \text{Correction Bolus}$$
  - **Safety Features**:
    - Insulin on Board (IOB) estimation / decay curve to prevent insulin stacking and hypoglycemia.
    - Configurable minimum step increments (e.g., 0.5 unit or 1.0 unit pen resolution).
    - Time-of-day ratios (morning, lunch, dinner CIR variations).
    - Clear medical disclaimers and recommendation confirmation prompts.

### 📄 2. Medical Data Export & Clinical Reports
- **CSV & JSON Export**: Unlocked full data backup for user ownership and spreadsheet analysis.
- **PDF Ambulatory Glucose Profile (AGP) Report**:
  - Multi-week summary report ready for endocrinology visits.
  - Formatted Time-in-Range percentages, average glucose, standard deviation (glycemic variability), meal averages, and daily dose totals.

### ⏰ 3. Smart Reminders & Alerts
- **Post-Prandial Testing Reminder**: Notification scheduled 2 hours after logging a meal or pre-meal dose.
- **Basal / Long-Acting Reminder**: Daily scheduled reminder for long-acting injection.
- **Customizable Testing Schedule**: Tailored prompts for users on intensive insulin therapy.

### ☁️ 4. Encrypted Local & Cloud Backups
- Encrypted export files with user-defined passwords.
- Optional seamless sync via Google Drive / private cloud storage while preserving offline-first guarantees.
