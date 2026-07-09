/**
 * FooterSectionComponent.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Renders one footer section (title + list of links).
 *
 * Link behaviour:
 *  • Routes in [AppConfig.Links.INTERNAL_ROUTES] (/, /predictions, /analytics)
 *    invoke [onNavigate] so the app navigates without leaving.
 *  • All other hrefs open [AppConfig.Links.WEB_BASE_URL]{href} in the browser.
 */
package com.example.xai_flows.ui.components.footer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xai_flows.core.config.AppConfig
import com.example.xai_flows.core.utils.IntentUtils
import com.example.xai_flows.ui.models.FooterSection

@Composable
fun FooterSectionComponent(
    section: FooterSection,
    /** Called with the route string (e.g. "/predictions") for in-app navigation. */
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current

    Column {
        Text(
            text     = section.title,
            color    = Color.White,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        section.links.forEach { link ->
            val interactionSource = remember { MutableInteractionSource() }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication        = ripple(color = Color(0xFF60A5FA)),
                        role              = Role.Button,
                        onClickLabel      = link.name
                    ) {
                        if (link.href in AppConfig.Links.INTERNAL_ROUTES) {
                            // In-app navigation
                            onNavigate(link.href)
                        } else {
                            // Open on the Vercel web frontend
                            IntentUtils.openUrl(context, AppConfig.Links.WEB_BASE_URL + link.href)
                        }
                    }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector        = link.icon,
                    contentDescription = null,
                    tint               = Color(0xFF9CA3AF),
                    modifier           = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = link.name, color = Color(0xFF9CA3AF), fontSize = 12.sp)
            }
        }
    }
}
