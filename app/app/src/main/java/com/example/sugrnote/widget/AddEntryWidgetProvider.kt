package com.example.sugrnote.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.sugrnote.MainActivity
import com.example.sugrnote.R
import com.example.sugrnote.data.local.AppDatabase
import com.example.sugrnote.data.repository.GlucoseRepository
import com.example.sugrnote.data.settings.SettingsRepository
import com.example.sugrnote.domain.model.GlucoseStatus
import com.example.sugrnote.domain.util.GlucoseUnitConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddEntryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Run update asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context.applicationContext)
            val glucoseRepo = GlucoseRepository(db.glucoseEntryDao())
            val settingsRepo = SettingsRepository(context.applicationContext)

            val latestEntry = glucoseRepo.getLatestEntry()
            val prefs = settingsRepo.preferencesFlow.first()

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, latestEntry, prefs)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, AddEntryWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        latestEntry: com.example.sugrnote.data.local.GlucoseEntry?,
        prefs: com.example.sugrnote.data.settings.UserPreferences
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.sugrnote.ACTION_ADD_ENTRY"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val views = RemoteViews(context.packageName, R.layout.widget_add_entry)
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        if (latestEntry != null) {
            val status = GlucoseStatus.fromValue(
                latestEntry.glucoseMgDl,
                prefs.lowThresholdMgDl,
                prefs.highThresholdMgDl
            )
            
            val statusLabel = when (status) {
                GlucoseStatus.LOW -> "Low"
                GlucoseStatus.IN_RANGE -> "In Range"
                GlucoseStatus.HIGH -> "High"
            }
            
            val bgColor = when (status) {
                GlucoseStatus.LOW -> android.graphics.Color.parseColor("#E53935")
                GlucoseStatus.IN_RANGE -> android.graphics.Color.parseColor("#43A047")
                GlucoseStatus.HIGH -> android.graphics.Color.parseColor("#FF8F00")
            }

            val formattedValue = GlucoseUnitConverter.format(latestEntry.glucoseMgDl, prefs.glucoseUnit)
            
            views.setTextViewText(R.id.widget_text_reading, formattedValue)
            views.setTextViewText(R.id.widget_text_unit, prefs.glucoseUnit.displayLabel)
            views.setTextViewText(R.id.widget_text_period, latestEntry.period.displayLabel)
            
            val instant = java.time.Instant.ofEpochMilli(latestEntry.dateTime)
            val zoned = instant.atZone(java.time.ZoneId.systemDefault())
            views.setTextViewText(R.id.widget_text_time, com.example.sugrnote.domain.util.DateTimeUtils.formatDateTime(zoned, prefs))
            
            // Note: Since widget_add_bg is a shape drawable, we can tint its background dynamically
            // Requires API 31+ for setIcon, but we can just use setColorFilter on the background
            views.setInt(R.id.widget_container, "setBackgroundColor", bgColor)
        } else {
            views.setTextViewText(R.id.widget_text_reading, "--")
            views.setTextViewText(R.id.widget_text_unit, "")
            views.setTextViewText(R.id.widget_text_period, "")
            views.setTextViewText(R.id.widget_text_time, "")
            // Default primary color (Blue40)
            views.setInt(R.id.widget_container, "setBackgroundColor", android.graphics.Color.parseColor("#1A73E8"))
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.example.sugrnote.ACTION_UPDATE_WIDGET"
        
        fun sendUpdateBroadcast(context: Context) {
            val intent = Intent(context, AddEntryWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }
}
