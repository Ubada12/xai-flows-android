/**
 * CareersScreen.kt — mirrors frontend's careers-page.tsx natively.
 * Content lives in ui/models/SiteContent.kt (CAREER_POSITIONS).
 */
package com.example.xai_flows.ui.screens.site

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Work
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
import com.example.xai_flows.ui.models.CAREER_POSITIONS
import com.example.xai_flows.ui.models.COMPANY_INFO

@Composable
fun CareersScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    SitePageScaffold(
        title = "Careers",
        icon = Icons.Filled.Work,
        subtitle = "Join us in building early-warning systems that save lives. We are looking for " +
            "passionate engineers, scientists, and analysts who care about climate resilience.",
        onBack = onBack
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CAREER_POSITIONS.forEach { position ->
                BadgedInfoCard(
                    title = position.title,
                    subtitle = "${position.department} · ${position.type} · ${position.location}",
                    trailing = {
                        Button(
                            onClick = { IntentUtils.openEmail(context, COMPANY_INFO.emailCareers) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text(text = "Apply Now", color = Color.White, fontSize = 13.sp)
                        }
                    }
                )
            }
        }
    }
}
