/**
 * TeamScreen.kt — mirrors frontend's team-page.tsx natively.
 * Content lives in ui/models/SiteContent.kt (TEAM_MEMBERS) — edit there,
 * not here, to add/update a team member.
 */
package org.ubada.xaiflows.ui.screens.site

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.ubada.xaiflows.ui.components.site.BadgedInfoCard
import org.ubada.xaiflows.ui.components.site.SitePageScaffold
import org.ubada.xaiflows.ui.models.TEAM_MEMBERS

@Composable
fun TeamScreen(onBack: () -> Unit) {
    SitePageScaffold(
        title = "Our Team",
        icon = Icons.Filled.Groups,
        subtitle = "A multidisciplinary team combining hydrology, machine learning, GIS, and software " +
            "engineering to build a world-class flood warning system.",
        onBack = onBack
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TEAM_MEMBERS.forEach { member ->
                BadgedInfoCard(
                    title = member.name,
                    subtitle = member.role,
                    description = member.description,
                    icon = member.icon
                )
            }
        }
    }
}
