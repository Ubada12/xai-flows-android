/**
 * EventsScreen.kt — mirrors frontend's events-page.tsx natively.
 * Content lives in ui/models/SiteContent.kt (SITE_EVENTS).
 */
package com.example.xai_flows.ui.screens.site

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.xai_flows.ui.components.site.BadgedInfoCard
import com.example.xai_flows.ui.components.site.SitePageScaffold
import com.example.xai_flows.ui.models.SITE_EVENTS

@Composable
fun EventsScreen(onBack: () -> Unit) {
    SitePageScaffold(
        title = "Events",
        icon = Icons.Filled.Event,
        onBack = onBack
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SITE_EVENTS.forEach { event ->
                BadgedInfoCard(
                    title = event.title,
                    subtitle = "${event.date} · ${event.location}",
                    description = event.description,
                    badgeText = event.type,
                    badgeColor = Color(0xFFDBEAFE),
                    badgeTextColor = Color(0xFF1D4ED8)
                )
            }
        }
    }
}
