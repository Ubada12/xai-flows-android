/**
 * FooterMobile.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Full-width footer rendered at the bottom of scrollable screens.
 *
 * [onNavigate] is forwarded to [FooterSectionComponent] so that in-app routes
 * (/, /predictions, /analytics) trigger local navigation instead of opening
 * the browser.
 */
package org.ubada.xaiflows.ui.components.common

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
import org.ubada.xaiflows.ui.components.footer.CompanyInfoComponent
import org.ubada.xaiflows.ui.components.footer.ContactInfoComponent
import org.ubada.xaiflows.ui.components.footer.FooterSectionComponent
import org.ubada.xaiflows.ui.components.footer.SocialIconComponent
import org.ubada.xaiflows.ui.models.COMPANY_INFO
import org.ubada.xaiflows.ui.models.FOOTER_SECTIONS
import org.ubada.xaiflows.ui.models.SOCIAL_LINKS

@Preview(showBackground = true)
@Composable
fun FooterMobile(
    /** Called with the route string (e.g. "/predictions") when an in-app link is tapped. */
    onNavigate: (String) -> Unit = {}
) {
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
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Company Info + subscribe form
            CompanyInfoComponent()

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Contact Info (email / phone / address — all tappable)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ContactInfoComponent()
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Platform + Company sections
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FooterSectionComponent(FOOTER_SECTIONS[0], onNavigate) // Platform
                }
                Column(modifier = Modifier.weight(1f)) {
                    FooterSectionComponent(FOOTER_SECTIONS[1], onNavigate) // Company
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Support + Legal sections
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FooterSectionComponent(FOOTER_SECTIONS[2], onNavigate) // Support
                }
                Column(modifier = Modifier.weight(1f)) {
                    FooterSectionComponent(FOOTER_SECTIONS[3], onNavigate) // Legal
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Social icons + copyright
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    text     = "© ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} ${COMPANY_INFO.name}. All rights reserved.",
                    color    = Color(0xFF9CA3AF),
                    fontSize = 10.sp
                )
            }
        }
    }
}
