package com.example.sugrnote.ui.overview.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    entry: GlucoseEntry?,
    prefs: UserPreferences,
    shadowColor: Color,
    onEntryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = entry?.let {
        when (GlucoseStatus.fromValue(it.glucoseMgDl, prefs.lowThresholdMgDl, prefs.highThresholdMgDl)) {
            GlucoseStatus.LOW -> StatusLow
            GlucoseStatus.IN_RANGE -> StatusInRange
            GlucoseStatus.HIGH -> StatusHigh
        }
    }
    val bgColor = statusColor ?: MaterialTheme.colorScheme.surface
    val contentColor = if (statusColor != null) Color.White else MaterialTheme.colorScheme.onSurface
    val variantColor = if (statusColor != null) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = if (statusColor != null) Color.White else MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = CardDefaults.shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            ),
        onClick = { entry?.let { onEntryClick(it.id) } },
        colors = CardDefaults.cardColors(
            containerColor = bgColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Latest Reading",
                style = MaterialTheme.typography.labelLarge,
                color = variantColor
            )
            Spacer(Modifier.height(8.dp))

            if (entry != null) {
                Text(
                    GlucoseUnitConverter.format(entry.glucoseMgDl, prefs.glucoseUnit),
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    prefs.glucoseUnit.displayLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = variantColor
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    entry.period.displayLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryColor
                )

                Spacer(Modifier.height(4.dp))

                val zoned = Instant.ofEpochMilli(entry.dateTime).atZone(ZoneId.systemDefault())
                Text(
                    DateTimeUtils.formatRelativeDateTime(zoned, prefs),
                    style = MaterialTheme.typography.bodySmall,
                    color = variantColor
                )
            } else {
                Text(
                    "No readings yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = variantColor
                )
            }
        }
    }
}
