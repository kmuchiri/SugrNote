package com.example.sugrnote.ui.export

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.data.repository.GlucoseRepository
import com.example.sugrnote.data.settings.SettingsRepository
import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.export.CsvExportGenerator
import com.example.sugrnote.domain.export.ExportDataCalculator
import com.example.sugrnote.domain.export.ExportReportData
import com.example.sugrnote.domain.export.HtmlReportGenerator
import com.example.sugrnote.domain.export.PrintDocumentHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed class ExportState {
    object Idle : ExportState()
    object Loading : ExportState()
    data class Success(val message: String) : ExportState()
    data class Error(val error: String) : ExportState()
}

class ExportViewModel(
    private val glucoseRepository: GlucoseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _windowMonths = MutableStateFlow(3)
    val windowMonths: StateFlow<Int> = _windowMonths

    private val _startDate = MutableStateFlow(LocalDate.now().minusMonths(3))
    val startDate: StateFlow<LocalDate> = _startDate

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    val effectiveEndDate: StateFlow<LocalDate> = combine(_startDate, _windowMonths) { start, months ->
        val end = start.plusMonths(months.toLong())
        val today = LocalDate.now()
        if (end.isAfter(today)) today else end
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalDate.now())

    val canSlideForward: StateFlow<Boolean> = combine(effectiveEndDate, _startDate) { end, _ ->
        end.isBefore(LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val matchingEntries: StateFlow<List<GlucoseEntry>> = combine(_startDate, effectiveEndDate) { start, end ->
        val zoneId = ZoneId.systemDefault()
        val startMillis = start.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = end.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
        startMillis to endMillis
    }.flatMapLatest { (startMillis, endMillis) ->
        glucoseRepository.observeEntriesBetween(startMillis, endMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportData: StateFlow<ExportReportData?> = combine(
        matchingEntries,
        _startDate,
        effectiveEndDate,
        _windowMonths,
        userPreferences
    ) { entries, start, end, months, prefs ->
        ExportDataCalculator.generateReportData(entries, start, end, months, prefs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setWindowMonths(months: Int) {
        val clampedMonths = months.coerceIn(1, 12)
        _windowMonths.value = clampedMonths
    }

    fun setStartDate(date: LocalDate) {
        val today = LocalDate.now()
        val validDate = if (date.isAfter(today)) today else date
        _startDate.value = validDate
    }

    fun setAnchorToToday(months: Int) {
        val clampedMonths = months.coerceIn(1, 12)
        _windowMonths.value = clampedMonths
        _startDate.value = LocalDate.now().minusMonths(clampedMonths.toLong())
    }

    fun slidePrevious() {
        val months = _windowMonths.value.toLong()
        _startDate.value = _startDate.value.minusMonths(months)
    }

    fun slideNext() {
        val months = _windowMonths.value.toLong()
        val currentStart = _startDate.value
        val currentEnd = effectiveEndDate.value
        val today = LocalDate.now()

        if (!currentEnd.isBefore(today)) return

        val potentialNewStart = currentStart.plusMonths(months)
        _startDate.value = if (potentialNewStart.isAfter(today)) {
            today.minusMonths(months)
        } else {
            potentialNewStart
        }
    }

    fun clearState() {
        _exportState.value = ExportState.Idle
    }

    fun printPdf(context: Context) {
        val data = reportData.value ?: return
        val prefs = userPreferences.value
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val html = withContext(Dispatchers.Default) {
                    HtmlReportGenerator.generate(data, prefs)
                }
                val jobName = "SugrNote_${data.periodStartDate}_to_${data.periodEndDate}"
                PrintDocumentHelper.printHtml(context, html, jobName)
                _exportState.value = ExportState.Success("Sent to system print spooler.")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.localizedMessage ?: "Failed to generate print document.")
            }
        }
    }

    fun shareHtml(context: Context) {
        val data = reportData.value ?: return
        val prefs = userPreferences.value
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val html = withContext(Dispatchers.Default) {
                    HtmlReportGenerator.generate(data, prefs)
                }
                val fileName = "SugrNote_Report_${data.periodStartDate}_${data.periodEndDate}.html"
                PrintDocumentHelper.saveToCacheAndShare(
                    context = context,
                    content = html,
                    fileName = fileName,
                    mimeType = "text/html",
                    title = "Share SugrNote Offline Web Report"
                )
                _exportState.value = ExportState.Success("Report ready to share.")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.localizedMessage ?: "Failed to export HTML report.")
            }
        }
    }

    fun shareCsv(context: Context) {
        val data = reportData.value ?: return
        val prefs = userPreferences.value
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val csv = withContext(Dispatchers.Default) {
                    CsvExportGenerator.generate(data, prefs)
                }
                val fileName = "SugrNote_Data_${data.periodStartDate}_${data.periodEndDate}.csv"
                PrintDocumentHelper.saveToCacheAndShare(
                    context = context,
                    content = csv,
                    fileName = fileName,
                    mimeType = "text/csv",
                    title = "Share SugrNote CSV Data"
                )
                _exportState.value = ExportState.Success("CSV file ready to share.")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.localizedMessage ?: "Failed to export CSV.")
            }
        }
    }
}
