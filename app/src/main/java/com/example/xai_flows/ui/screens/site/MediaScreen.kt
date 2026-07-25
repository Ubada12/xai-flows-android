/**
 * MediaScreen.kt — mirrors frontend's media-page.tsx natively.
 * Content lives in ui/models/SiteContent.kt (PRESS_ITEMS).
 */
package com.example.xai_flows.ui.screens.site

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xai_flows.core.utils.IntentUtils
import com.example.xai_flows.ui.components.site.BadgedInfoCard
import com.example.xai_flows.ui.components.site.SitePageScaffold
import com.example.xai_flows.ui.models.COMPANY_INFO
import com.example.xai_flows.ui.models.PRESS_ITEMS

@Composable
fun MediaScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    SitePageScaffold(
        title = "Press & Media",
        icon = Icons.Filled.Newspaper,
        onBack = onBack
    ) {
        Text(
            text = "Media enquiries: ${COMPANY_INFO.emailMedia}",
            fontSize = 13.sp,
            color = Color(0xFF4B5563),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Recent Coverage",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PRESS_ITEMS.forEach { item ->
                BadgedInfoCard(
                    title = item.title,
                    subtitle = "${item.outlet} · ${item.date}"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tapping the whole banner opens the mail app with "Press Kit
        // Request" pre-filled — mirrors the frontend's inline mailto link
        // inside the same press-kit paragraph.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFDBEAFE), RoundedCornerShape(16.dp))
                .clickable {
                    IntentUtils.openEmail(context, COMPANY_INFO.email)
                }
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = "Press Kit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                Text(
                    text = "Logos, screenshots, and brand guidelines are available on request. " +
                        "Tap to email ${COMPANY_INFO.email} with subject \"Press Kit Request\".",
                    fontSize = 12.sp,
                    color = Color(0xFF4B5563),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
