/**
 * TermsOfServiceScreen.kt — mirrors frontend's terms-of-service-page.tsx
 * natively. Sections live in ui/models/SiteContent.kt (SITE_TERMS_SECTIONS);
 * the final "Contact" section is appended here so it always uses the live
 * COMPANY_INFO.email value.
 */
package com.example.xai_flows.ui.screens.site

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
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
import com.example.xai_flows.ui.models.SITE_TERMS_SECTIONS
import com.example.xai_flows.ui.models.TERMS_OF_SERVICE_LAST_UPDATED

@Composable
fun TermsOfServiceScreen(onBack: () -> Unit) {
    val allSections = remember {
        SITE_TERMS_SECTIONS + LegalSection(
            heading = "9. Contact",
            body = "For questions about these Terms, please contact ${COMPANY_INFO.email}."
        )
    }

    SitePageScaffold(
        title = "Terms of Service",
        icon = Icons.Filled.Description,
        onBack = onBack
    ) {
        Text(
            text = TERMS_OF_SERVICE_LAST_UPDATED,
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
