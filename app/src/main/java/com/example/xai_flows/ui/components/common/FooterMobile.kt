package com.example.xai_flows.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xai_flows.ui.components.footer.CompanyInfoComponent
import com.example.xai_flows.ui.components.footer.ContactInfoComponent
import com.example.xai_flows.ui.components.footer.FooterSectionComponent
import com.example.xai_flows.ui.components.footer.SocialIconComponent
import com.example.xai_flows.ui.models.COMPANY_INFO
import com.example.xai_flows.ui.models.FOOTER_SECTIONS
import com.example.xai_flows.ui.models.SOCIAL_LINKS

@Preview(showBackground = true)
@Composable
fun FooterMobile() {
    Box(
        modifier = Modifier
            .wrapContentHeight()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF111827), Color(0xFF1F2937), Color.Black)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .padding(horizontal = 16.dp, vertical = 20.dp), // reduced bottom padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top: Company Info
            CompanyInfoComponent()

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Row: Contact Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ContactInfoComponent()
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Row: Platform + Company
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FooterSectionComponent(FOOTER_SECTIONS[0]) // Platform
                }

                Column(modifier = Modifier.weight(1f)) {
                    FooterSectionComponent(FOOTER_SECTIONS[1]) // Company
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Row: Support + Legal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FooterSectionComponent(FOOTER_SECTIONS[2]) // Support
                }

                Column(modifier = Modifier.weight(1f)) {
                    FooterSectionComponent(FOOTER_SECTIONS[3]) // Legal
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Social Links + Copyright
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SOCIAL_LINKS.forEach { social ->
                        SocialIconComponent(social)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "© ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} ${COMPANY_INFO.name}. All rights reserved.",
                    color = Color(0xFF9CA3AF),
                    fontSize = 10.sp
                )
            }
        }
    }
}


