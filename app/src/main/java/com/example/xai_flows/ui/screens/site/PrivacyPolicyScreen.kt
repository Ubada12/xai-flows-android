/**
 * PrivacyPolicyScreen.kt — mirrors frontend's privacy-policy-page.tsx
 * natively. Sections live in ui/models/SiteContent.kt (SITE_PRIVACY_SECTIONS);
 * the final "Contact" section is appended here so it always uses the live
 * COMPANY_INFO.email value.
 */
package com.example.xai_flows.ui.screens.site

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xai_flows.ui.components.site.SectionCard
import com.example.xai_flows.ui.components.site.SitePageScaffold
import com.example.xai_flows.ui.models.COMPANY_INFO
import com.example.xai_flows.ui.models.LegalSection
import com.example.xai_flows.ui.models.PRIVACY_POLICY_LAST_UPDATED
import com.example.xai_flows.ui.models.SITE_PRIVACY_SECTIONS

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val allSections = remember {
        SITE_PRIVACY_SECTIONS + LegalSection(
            heading = "7. Contact",
            body = "For privacy-related enquiries, please email ${COMPANY_INFO.email}."
        )
    }

    SitePageScaffold(
        title = "Privacy Policy",
        icon = Icons.Filled.Security,
        onBack = onBack
    ) {
        Text(
            text = PRIVACY_POLICY_LAST_UPDATED,
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            allSections.forEach { section ->
                SectionCard(heading = section.heading, body = section.body)
            }
        }
    }
}
