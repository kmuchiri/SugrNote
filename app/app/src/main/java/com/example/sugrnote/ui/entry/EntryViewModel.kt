package com.example.sugrnote.ui.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.data.repository.GlucoseRepository
import com.example.sugrnote.data.settings.SettingsRepository
import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.model.EntrySource
import com.example.sugrnote.domain.model.GlucoseUnit
import com.example.sugrnote.domain.model.InsulinType
import com.example.sugrnote.domain.model.Period
import com.example.sugrnote.domain.util.GlucoseUnitConverter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class BasePeriod(val displayLabel: String) {
    FASTING("Fasting"),
    BEFORE_MEAL("Before Meal"),
    AFTER_MEAL("After Meal"),
    BEFORE_SLEEP("Before Sleep"),
    BEFORE_EXERCISE("Before Exercise"),
    AFTER_EXERCISE("After Exercise"),
    RANDOM("Random")
}

enum class MealType(val displayLabel: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack")
}

class EntryViewModel(
    private val repository: GlucoseRepository,
    settingsRepository: SettingsRepository,
    private val applicationContext: android.content.Context,
    private val entryId: Long? = null
) : ViewModel() {

    private val preferencesFlow = settingsRepository.preferencesFlow

    val userPreferences: StateFlow<UserPreferences> = preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    var date by mutableStateOf(LocalDate.now())
        private set
    var time by mutableStateOf(LocalTime.now())
        private set
    var glucoseValueText by mutableStateOf("")
        private set
    var period by mutableStateOf(Period.RANDOM)
        private set
    var basePeriod by mutableStateOf(BasePeriod.RANDOM)
        private set
    var mealType by mutableStateOf(MealType.BREAKFAST)
        private set
    var insulinType by mutableStateOf(InsulinType.NONE)
        private set
    var longActingUnitsText by mutableStateOf("")
        private set
    var shortActingUnitsText by mutableStateOf("")
        private set
    var hasFood by mutableStateOf(false)
        private set
    var carbAmountText by mutableStateOf("")
        private set

    // Validation errors
    var glucoseError by mutableStateOf<String?>(null)
        private set
    var longActingError by mutableStateOf<String?>(null)
        private set
    var shortActingError by mutableStateOf<String?>(null)
        private set
    var carbError by mutableStateOf<String?>(null)
        private set

    var isSaved by mutableStateOf(false)
        private set

    var showUnusualValueDialog by mutableStateOf(false)
        private set

    private var isEditMode = false

    init {
        if (entryId != null) {
            isEditMode = true
            loadEntry(entryId)
        }
    }

    private fun loadEntry(id: Long) {
        viewModelScope.launch {
            // Use preferencesFlow.first() to get real DataStore values
            // (not userPreferences.value which may still be the stateIn default)
            val prefs = preferencesFlow.first()
            repository.getEntryById(id)?.let { entry ->
                val instant = Instant.ofEpochMilli(entry.dateTime)
                val zoneId = ZoneId.systemDefault()
                date = instant.atZone(zoneId).toLocalDate()
                time = instant.atZone(zoneId).toLocalTime()

                val unit = prefs.glucoseUnit
                glucoseValueText = when (unit) {
                    GlucoseUnit.MG_DL -> entry.glucoseMgDl.toInt().toString()
                    GlucoseUnit.MMOL_L -> String.format(
                        "%.1f",
                        GlucoseUnitConverter.mgDlToMmolL(entry.glucoseMgDl)
                    )
                }

                period = entry.period
                basePeriod = when (period) {
                    Period.FASTING -> BasePeriod.FASTING
                    Period.BEFORE_BREAKFAST, Period.BEFORE_LUNCH, Period.BEFORE_DINNER, Period.BEFORE_SNACK -> BasePeriod.BEFORE_MEAL
                    Period.AFTER_BREAKFAST, Period.AFTER_LUNCH, Period.AFTER_DINNER, Period.AFTER_SNACK -> BasePeriod.AFTER_MEAL
                    Period.BEFORE_SLEEP -> BasePeriod.BEFORE_SLEEP
                    Period.BEFORE_EXERCISE -> BasePeriod.BEFORE_EXERCISE
                    Period.AFTER_EXERCISE -> BasePeriod.AFTER_EXERCISE
                    Period.RANDOM -> BasePeriod.RANDOM
                }
                mealType = when (period) {
                    Period.BEFORE_BREAKFAST, Period.AFTER_BREAKFAST -> MealType.BREAKFAST
                    Period.BEFORE_LUNCH, Period.AFTER_LUNCH -> MealType.LUNCH
                    Period.BEFORE_DINNER, Period.AFTER_DINNER -> MealType.DINNER
                    Period.BEFORE_SNACK, Period.AFTER_SNACK -> MealType.SNACK
                    else -> MealType.BREAKFAST
                }
                
                insulinType = entry.insulinType
                longActingUnitsText = entry.longActingUnits?.toString() ?: ""
                shortActingUnitsText = entry.shortActingUnits?.toString() ?: ""
                hasFood = entry.hasFood
                carbAmountText = entry.carbAmount?.toString() ?: ""
            }
        }
    }

    fun onDateChanged(newDate: LocalDate) { date = newDate }
    fun onTimeChanged(newTime: LocalTime) { time = newTime }
    fun onGlucoseValueChanged(text: String) {
        glucoseValueText = text
        glucoseError = null
    }

    fun onBasePeriodChanged(newPeriod: BasePeriod) {
        basePeriod = newPeriod
        when (newPeriod) {
            BasePeriod.BEFORE_MEAL -> onHasFoodChanged(true)
            BasePeriod.FASTING, BasePeriod.RANDOM, BasePeriod.BEFORE_SLEEP,
            BasePeriod.BEFORE_EXERCISE, BasePeriod.AFTER_EXERCISE -> onHasFoodChanged(false)
            BasePeriod.AFTER_MEAL -> { /* retain current food selection */ }
        }
        updateActualPeriod()
    }

    fun onMealTypeChanged(newMealType: MealType) {
        mealType = newMealType
        updateActualPeriod()
    }

    private fun updateActualPeriod() {
        period = when (basePeriod) {
            BasePeriod.FASTING -> Period.FASTING
            BasePeriod.BEFORE_SLEEP -> Period.BEFORE_SLEEP
            BasePeriod.BEFORE_EXERCISE -> Period.BEFORE_EXERCISE
            BasePeriod.AFTER_EXERCISE -> Period.AFTER_EXERCISE
            BasePeriod.RANDOM -> Period.RANDOM
            BasePeriod.BEFORE_MEAL -> when (mealType) {
                MealType.BREAKFAST -> Period.BEFORE_BREAKFAST
                MealType.LUNCH -> Period.BEFORE_LUNCH
                MealType.DINNER -> Period.BEFORE_DINNER
                MealType.SNACK -> Period.BEFORE_SNACK
            }
            BasePeriod.AFTER_MEAL -> when (mealType) {
                MealType.BREAKFAST -> Period.AFTER_BREAKFAST
                MealType.LUNCH -> Period.AFTER_LUNCH
                MealType.DINNER -> Period.AFTER_DINNER
                MealType.SNACK -> Period.AFTER_SNACK
            }
        }
    }

    fun onPeriodChanged(newPeriod: Period) { period = newPeriod }
    fun onInsulinTypeChanged(newType: InsulinType) {
        insulinType = newType
        // Clear hidden fields
        if (newType != InsulinType.LONG_ACTING && newType != InsulinType.BOTH) {
            longActingUnitsText = ""
            longActingError = null
        }
        if (newType != InsulinType.SHORT_ACTING && newType != InsulinType.BOTH) {
            shortActingUnitsText = ""
            shortActingError = null
        }
    }
    fun onLongActingUnitsChanged(text: String) {
        longActingUnitsText = text
        longActingError = null
    }
    fun onShortActingUnitsChanged(text: String) {
        shortActingUnitsText = text
        shortActingError = null
    }
    fun onHasFoodChanged(value: Boolean) {
        hasFood = value
        if (!value) {
            carbAmountText = ""
            carbError = null
        }
    }
    fun onCarbAmountChanged(text: String) {
        carbAmountText = text
        carbError = null
    }

    fun onDismissUnusualValueDialog() {
        showUnusualValueDialog = false
    }

    fun onConfirmUnusualValue() {
        showUnusualValueDialog = false
        performSave(skipRangeCheck = true)
    }

    fun saveEntry() {
        performSave(skipRangeCheck = false)
    }

    private fun performSave(skipRangeCheck: Boolean) {
        val unit = userPreferences.value.glucoseUnit

        // Validate glucose
        val glucoseDisplayVal = glucoseValueText.toFloatOrNull()
        if (glucoseDisplayVal == null || glucoseDisplayVal <= 0) {
            glucoseError = "Enter a valid glucose value"
            return
        }
        val glucoseMgDl = GlucoseUnitConverter.toMgDl(glucoseDisplayVal, unit)

        if (!skipRangeCheck && (glucoseMgDl < 20f || glucoseMgDl > 600f)) {
            showUnusualValueDialog = true
            return
        }

        // Validate insulin fields
        val needsLong = insulinType == InsulinType.LONG_ACTING || insulinType == InsulinType.BOTH
        val needsShort = insulinType == InsulinType.SHORT_ACTING || insulinType == InsulinType.BOTH

        var longUnits: Float? = null
        if (needsLong) {
            longUnits = longActingUnitsText.toFloatOrNull()
            if (longUnits == null || longUnits < 0) {
                longActingError = "Enter valid units"
                return
            }
        }

        var shortUnits: Float? = null
        if (needsShort) {
            shortUnits = shortActingUnitsText.toFloatOrNull()
            if (shortUnits == null || shortUnits < 0) {
                shortActingError = "Enter valid units"
                return
            }
        }

        // Validate carbs
        var carbs: Float? = null
        if (hasFood) {
            carbs = carbAmountText.toFloatOrNull()
            if (carbs == null || carbs < 0) {
                carbError = "Enter valid carb amount"
                return
            }
        }

        // Build dateTime
        val zonedDateTime = date.atTime(time).atZone(ZoneId.systemDefault())
        val epochMillis = zonedDateTime.toInstant().toEpochMilli()

        val entry = GlucoseEntry(
            id = entryId ?: 0,
            glucoseMgDl = glucoseMgDl,
            dateTime = epochMillis,
            period = period,
            insulinType = insulinType,
            longActingUnits = longUnits,
            shortActingUnits = shortUnits,
            hasFood = hasFood,
            carbAmount = carbs,
            sourceType = EntrySource.MANUAL
        )

        viewModelScope.launch {
            if (isEditMode) {
                repository.updateEntry(entry)
            } else {
                repository.insertEntry(entry)
            }
            
            // Notify widget to update
            com.example.sugrnote.widget.AddEntryWidgetProvider.sendUpdateBroadcast(applicationContext)
            
            isSaved = true
        }
    }
}
