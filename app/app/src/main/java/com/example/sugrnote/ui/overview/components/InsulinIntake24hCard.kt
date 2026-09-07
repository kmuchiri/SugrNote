package com.example.sugrnote.ui.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.sugrnote.data.local.GlucoseEntry
import com.example.sugrnote.data.settings.UserPreferences
import com.example.sugrnote.domain.util.DateTimeUtils
import com.example.sugrnote.ui.overview.InsulinStats24h
import java.time.Instant
import java.time.ZoneId

@Composable
fun InsulinIntake24hCard(
    modifier: Modifier = Modifier,
    insulinStats24h: InsulinStats24h,
    latestLongActingEntry: GlucoseEntry?,
    latestShortActingEntry: GlucoseEntry?,
    prefs: UserPreferences,
    shadowColor: Color
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = CardDefaults.shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Insulin (24h)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            if (insulinStats24h.totalInjections > 0) {
                val formatInsulin = { value: Float -> if (value % 1 == 0f) value.toInt().toString() else String.format("%.1f", value) }

                // Total units
                Text(
                    "${formatInsulin(insulinStats24h.total)}u",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${insulinStats24h.totalInjections} injection(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                // Last Long Acting injection
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF9C27B0)))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Long Acting",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (latestLongActingEntry != null) {
                    val longZoned = Instant.ofEpochMilli(latestLongActingEntry.dateTime)
                        .atZone(ZoneId.systemDefault())
                    Text(
                        DateTimeUtils.formatRelativeDateTime(longZoned, prefs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                } else {
                    Text(
                        "None",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Last Short Acting injection
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2196F3)))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Short Acting",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (latestShortActingEntry != null) {
                    val shortZoned = Instant.ofEpochMilli(latestShortActingEntry.dateTime)
                        .atZone(ZoneId.systemDefault())
                    Text(
                        DateTimeUtils.formatRelativeDateTime(shortZoned, prefs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                } else {
                    Text(
                        "None",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }
            } else {
                Text(
                    "No injections",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
