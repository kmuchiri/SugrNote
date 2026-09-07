package com.example.sugrnote.ui.overview.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.model.GlucoseStatus
import com.example.sugrnote.domain.util.DateTimeUtils
import com.example.sugrnote.domain.util.GlucoseUnitConverter
import com.example.sugrnote.ui.theme.StatusHigh
import com.example.sugrnote.ui.theme.StatusInRange
import com.example.sugrnote.ui.theme.StatusLow
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatestReadingCard(
    modifier: Modifier = Modifier,
    latestEntry: GlucoseEntry?,
    prefs: UserPreferences,
    shadowColor: Color,
    onEntryClick: (Long) -> Unit
) {
    val latestStatusColor = latestEntry?.let { entry ->
        when (GlucoseStatus.fromValue(entry.glucoseMgDl, prefs.lowThresholdMgDl, prefs.highThresholdMgDl)) {
            GlucoseStatus.LOW -> StatusLow
            GlucoseStatus.IN_RANGE -> StatusInRange
            GlucoseStatus.HIGH -> StatusHigh
        }
    }
    val latestCardBgColor = latestStatusColor ?: MaterialTheme.colorScheme.surface
    val latestCardContentColor = if (latestStatusColor != null) Color.White else MaterialTheme.colorScheme.onSurface
    val latestCardVariantColor = if (latestStatusColor != null) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val latestCardPrimaryColor = if (latestStatusColor != null) Color.White else MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = CardDefaults.shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            ),
        onClick = {
            latestEntry?.let { onEntryClick(it.id) }
        },
        colors = CardDefaults.cardColors(
            containerColor = latestCardBgColor,
            contentColor = latestCardContentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Latest Reading",
                style = MaterialTheme.typography.labelLarge,
                color = latestCardVariantColor
            )
            Spacer(Modifier.height(8.dp))

            if (latestEntry != null) {
                val entry = latestEntry

                Text(
                    GlucoseUnitConverter.format(
                        entry.glucoseMgDl,
                        prefs.glucoseUnit
                    ),
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    prefs.glucoseUnit.displayLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = latestCardVariantColor
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    entry.period.displayLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = latestCardPrimaryColor
                )

                Spacer(Modifier.height(4.dp))

                val instant = Instant.ofEpochMilli(entry.dateTime)
                val zoned = instant.atZone(ZoneId.systemDefault())
                Text(
                    DateTimeUtils.formatRelativeDateTime(zoned, prefs),
                    style = MaterialTheme.typography.bodySmall,
                    color = latestCardVariantColor
                )
            } else {
                Text(
                    "No readings yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = latestCardVariantColor
                )
            }
        }
    }
}
