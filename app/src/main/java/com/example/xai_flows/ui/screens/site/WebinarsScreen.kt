/**
 * WebinarsScreen.kt — mirrors frontend's webinars-page.tsx natively.
 * Content lives in ui/models/SiteContent.kt (WEBINARS).
 */
package com.example.xai_flows.ui.screens.site

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xai_flows.core.utils.IntentUtils
import com.example.xai_flows.ui.components.site.BadgedInfoCard
import com.example.xai_flows.ui.components.site.SitePageScaffold
import com.example.xai_flows.ui.models.WEBINARS

@Composable
fun WebinarsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    SitePageScaffold(
        title = "Webinars",
        icon = Icons.Filled.Videocam,
        onBack = onBack
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WEBINARS.forEach { webinar ->
                BadgedInfoCard(
                    title = webinar.title,
                    subtitle = "${webinar.date} · ${webinar.duration}",
                    badgeText = if (webinar.available) "Recording Available" else "Upcoming",
                    badgeColor = if (webinar.available) Color(0xFFDCFCE7) else Color(0xFFF3F4F6),
                    badgeTextColor = if (webinar.available) Color(0xFF15803D) else Color(0xFF6B7280),
                    trailing = {
                        when {
                            webinar.available && webinar.recordingUrl != null -> Button(
                                onClick = { IntentUtils.openUrl(context, webinar.recordingUrl) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Text(text = "Watch Recording", color = Color.White, fontSize = 13.sp)
                            }
                            webinar.available -> Text(
                                text = "Coming Soon",
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp
                            )
                            else -> Unit // not yet held — no action to offer
                        }
                    }
                )
            }
        }
    }
}
