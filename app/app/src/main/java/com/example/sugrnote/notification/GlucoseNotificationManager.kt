package com.example.sugrnote.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import com.example.sugrnote.MainActivity
import com.example.sugrnote.R
import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.model.GlucoseStatus
import com.example.sugrnote.domain.util.GlucoseUnitConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages the persistent glucose reading notification.
 *
 * Uses [NotificationCompat.setColor] and [NotificationCompat.setColorized] to
 * colour the notification background based on the glucose range status.
 * `setColorized(true)` works reliably on ongoing notifications on most devices;
 * where it doesn't, the colour still tints the icon and accent.
 */
object GlucoseNotificationManager {

    private const val CHANNEL_ID = "glucose_reading_channel"
    private const val NOTIFICATION_ID = 1001

    // Match the theme Status colours (ARGB ints)
    private const val COLOR_LOW = 0xFFE53935.toInt()      // StatusLow
    private const val COLOR_IN_RANGE = 0xFF43A047.toInt()  // StatusInRange
    private const val COLOR_HIGH = 0xFFFF8F00.toInt()      // StatusHigh

    /** Must be called once (e.g. in onCreate) before posting notifications. */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Latest Glucose Reading",
            NotificationManager.IMPORTANCE_LOW   // no sound, visible in shade
        ).apply {
            description = "Persistent notification showing your most recent glucose reading"
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /**
     * Posts or updates the persistent notification for [entry].
     * Call this whenever the latest entry or preferences change,
     * and periodically to keep the relative timestamp fresh.
     */
    fun show(context: Context, entry: GlucoseEntry, prefs: UserPreferences) {
        val status = GlucoseStatus.fromValue(
            entry.glucoseMgDl,
            prefs.lowThresholdMgDl,
            prefs.highThresholdMgDl
        )

        val displayValue = GlucoseUnitConverter.format(entry.glucoseMgDl, prefs.glucoseUnit)
        val unitLabel = prefs.glucoseUnit.displayLabel
        val statusLabel = when (status) {
            GlucoseStatus.LOW -> "Low"
            GlucoseStatus.IN_RANGE -> "In Range"
            GlucoseStatus.HIGH -> "High"
        }
        val statusColor = when (status) {
            GlucoseStatus.LOW -> COLOR_LOW
            GlucoseStatus.IN_RANGE -> COLOR_IN_RANGE
            GlucoseStatus.HIGH -> COLOR_HIGH
        }

        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val absoluteTime = formatter.format(Date(entry.dateTime))

        // Tapping the notification opens the app
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_glucose)
            .setContentTitle("$displayValue $unitLabel · $statusLabel")
            .setContentText(absoluteTime)
            .setOngoing(true)
            .setColor(statusColor)
            .setColorized(true)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    /** Remove the notification (e.g. when all entries are deleted). */
    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(NOTIFICATION_ID)
    }
}
