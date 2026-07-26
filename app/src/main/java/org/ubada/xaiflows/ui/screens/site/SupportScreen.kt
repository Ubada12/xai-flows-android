/**
 * SupportScreen.kt — mirrors frontend's support-page.tsx natively.
 * Channels are built inline from COMPANY_INFO (FooterData.kt) rather than
 * SiteContent.kt, same convention the frontend itself uses (support-page.tsx
 * defines its CHANNELS array locally rather than in app.config.ts) since
 * each channel's action is code (an Intent), not pure content.
 */
package org.ubada.xaiflows.ui.screens.site

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ubada.xaiflows.core.utils.IntentUtils
import org.ubada.xaiflows.ui.components.site.ContactChannelCard
import org.ubada.xaiflows.ui.components.site.SitePageScaffold
import org.ubada.xaiflows.ui.models.COMPANY_INFO

@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    SitePageScaffold(
        title = "Support",
        icon = Icons.Filled.Headset,
        subtitle = "Our dedicated support team is here to help you get the most out of the XAI-FLOWS platform.",
        onBack = onBack
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ContactChannelCard(
                icon = Icons.Filled.Email,
                title = "Email Support",
                value = COMPANY_INFO.emailSupport,
                description = "Typical response within 24 hours, Monday – Friday.",
                onClick = { IntentUtils.openEmail(context, COMPANY_INFO.emailSupport) }
            )
            ContactChannelCard(
                icon = Icons.Filled.Phone,
                title = "Phone",
                value = COMPANY_INFO.phone,
                description = "Available 9 AM – 6 PM IST on business days.",
                onClick = { IntentUtils.openPhone(context, COMPANY_INFO.phone) }
            )
            ContactChannelCard(
                icon = Icons.Filled.Chat,
                title = "Technical Queries",
                value = "Submit via email",
                description = "Attach logs or screenshots to help us assist you faster.",
                onClick = { IntentUtils.openEmail(context, COMPANY_INFO.emailSupport) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFDBEAFE), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(text = "Our Office", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Text(
                text = COMPANY_INFO.address,
                fontSize = 13.sp,
                color = Color(0xFF4B5563),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
