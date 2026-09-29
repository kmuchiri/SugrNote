# SugrNote Roadmap & Upcoming Features

This document outlines the planned roadmap, technical specifications for features to be implemented, and the status of experimental features for SugrNote.

---

## 🧮 Priority 1: Carb-to-Insulin Ratio (CIR) Calculator & Bolus Advisor

### 1.1 Objective
Empower individuals with Type 1 or insulin-dependent Type 2 diabetes to calculate precise mealtime and correction boluses directly within SugrNote, reducing mental math fatigue, preventing dosing errors, and avoiding insulin stacking.

### 1.2 Mathematical Formulation

The bolus advisor will calculate recommended insulin doses using standard endocrinological clinical guidelines:

#### 1. Food Bolus (Meal Dose)
$$\text{Food Bolus (units)} = \frac{\text{Carbohydrate Intake (g)}}{\text{Carb-to-Insulin Ratio (CIR in g/u)}}$$

*Example: Eating 60g carbs with a CIR of 10g per unit $\rightarrow 60 / 10 = 6.0\text{ units}$.*

#### 2. Correction Bolus (High Blood Sugar Correction)
$$\text{Correction Bolus (units)} = \frac{\text{Current Blood Glucose} - \text{Target Blood Glucose}}{\text{Insulin Sensitivity Factor (ISF / Correction Factor)}}$$

*Example: Current BG is 220 mg/dL, Target BG is 100 mg/dL, and ISF is 40 mg/dL per unit $\rightarrow (220 - 100) / 40 = 3.0\text{ units}$.*  
*If Current BG $\le$ Target BG, the correction dose is $0$ (or negative if programmed for hypo adjustment).*

#### 3. Insulin On Board (IOB) Deduction
To prevent insulin stacking (taking additional insulin before previous doses have finished acting):
$$\text{Active IOB} = \text{Previous Dose} \times \left(1 - \frac{\text{Elapsed Time}}{\text{Duration of Insulin Action (DIA)}}\right)$$
$$\text{Total Recommended Bolus} = \text{Food Bolus} + \max(0, \text{Correction Bolus} - \text{Active IOB})$$

### 1.3 Feature Specifications & Settings

- **Individualized Ratios**:
  - Configurable CIR and ISF in user's active units (`mg/dL` or `mmol/L`).
  - Time-of-day ratio schedules (e.g., Morning CIR: 1:8, Lunch CIR: 1:10, Dinner CIR: 1:12).
- **Insulin Delivery Resolution**:
  - Dosing step increments matching user's pen or pump: `0.5 unit` (half-unit pens) or `1.0 unit` (standard pens).
  - Rounding logic: Conservative round-down or standard nearest rounding.
- **Entry Screen Integration**:
  - Quick "Calculate Bolus" button alongside the Carbohydrate and Short-Acting Insulin fields in the entry form.
  - Automatically populates the short-acting units field upon user confirmation.
- **Standalone Calculator Screen**:
  - A quick-access calculator utility screen accessible from the navigation or home menu for rapid "what-if" calculations without immediately creating a database log.
- **Safety Disclaimers**:
  - Clear medical disclaimers emphasizing that calculations are informational aids and clinical guidance from healthcare professionals takes precedence.

---

## 📄 Priority 2: Medical Data Export & Clinical Reports

### 2.1 PDF Ambulatory Glucose Profile (AGP) Report
- **Clinical Alignment**: Formatted following standard clinical AGP reporting guidelines.
- **Content**:
  - Time-in-Range (TIR) percentages: % Very Low (< 54 mg/dL), % Low (54–69 mg/dL), % Target (70–180 mg/dL), % High (181–250 mg/dL), and % Very High (> 250 mg/dL).
  - Glucose Management Indicator (GMI / estimated HbA1c).
  - Glycemic variability (Standard Deviation and Coefficient of Variation).
  - 24-hour overlay trend charts and mealtime averages.
  - Total daily dose (TDD) insulin averages and basal/bolus percentage splits.
- **Format**: Printable, high-resolution vector PDF shareable directly to endocrinologists or primary care providers.

### 2.2 CSV & JSON Data Export
- Comprehensive user data ownership export containing all logged timestamps, glucose readings, insulin doses, carbs, exercise, and notes.
- Import capability to restore from past exports.

---

## ⏰ Priority 3: Smart Contextual Reminders

### 3.1 Post-Prandial Blood Glucose Testing
- Automatically prompt an Android alarm/notification 2 hours after logging a meal or pre-meal insulin bolus to test post-prandial blood glucose.

### 3.2 Scheduled Basal Insulin Reminder
- Daily recurring notification alarm for taking long-acting basal insulin (e.g., every evening at 21:00).

### 3.3 Missed Entry Nudges
- Optional gentle reminders if no glucose readings have been recorded for an extended duration during the day.

---

## ☁️ Priority 4: Encrypted Local & Cloud Backups

### 4.1 Encrypted Local Backups
- Export password-protected AES-256 encrypted database archives to local device storage.

### 4.2 Optional Cloud Sync
- Seamless synchronization with user-owned Google Drive or WebDAV storage without requiring third-party accounts or sending plaintext health data to external servers.

---

## ⏸️ Shelved Features & Archive

### Optical Character Recognition (OCR) / Meter Screen Scanning
- **Current Status:** **Shelved for now**
- **Original Concept:** Use camera preview + on-device OCR (ML Kit / OpenCV) to capture glucose readings directly off physical meter LCD screens.
- **Evaluation & Challenges:**
  - Segmented LCD screens have high variability in font styles, decimal point indicators, backlighting, and glare.
  - User verification friction was higher than fast manual keypad entry.
  - Prioritized core clinical utilities (bolus calculation, trend analysis, export reports) that provide significantly higher daily health value.
- **Future Reconsideration:** The database schema retains `sourceType` and `imagePath` columns so camera integration can be revisited if specialized on-device vision models for medical displays become practical.
