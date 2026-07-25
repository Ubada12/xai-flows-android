/**
 * FooterSectionComponent.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Renders one footer section (title + list of links).
 *
 * Every link's href is an AppRoute.path (see FooterData.kt's FooterLink doc
 * comment) and always resolves to a real, native in-app screen — tapping
 * any footer link invokes [onNavigate], full stop. There is no longer a
 * "the page doesn't exist natively, open the website instead" branch: it
 * used to fall back to AppConfig.Links.WEB_BASE_URL + href for anything
 * not in a small internal-routes allowlist, which is exactly how nearly
 * every footer link ended up silently opening a browser to a 404 (see
 * AppRoute.kt's doc comment for the full story). Now that every footer
 * page has a real native screen (ui/screens/site/), that fallback has no
 * legitimate case left to handle, so it's gone rather than kept around as
 * dead code that could silently start misbehaving again later.
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xai_flows.ui.models.FooterSection

@Composable
fun FooterSectionComponent(
    section: FooterSection,
    /** Called with the route string (e.g. "/predictions") for in-app navigation. */
    onNavigate: (String) -> Unit = {}
) {
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
                        onNavigate(link.href)
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
