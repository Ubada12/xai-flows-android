/**
 * SitePageScaffold.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Shared page shell for every "site" screen — Team, Careers, Media, Events,
 * Webinars, FAQ, Support, Privacy Policy, Terms of Service (see
 * ui/screens/site/). Renders the light gradient background used by
 * HomePage, a "Back to Home" link, and a gradient icon + title header —
 * one composable instead of duplicating this markup 9 times, and matches
 * the frontend's own shell (every page in streamlit-frontend's
 * features/site/routes directory repeats this exact same
 * back-link + icon-badge + gradient-title header pattern).
 *
 * [onBack] always returns to AppRoute.HOME — same behaviour as every one
 * of the frontend's site pages, which all link "← Back to Home" to "/"
 * rather than a true browser-style back stack. MainActivity wires this to
 * `{ page.value = AppRoute.HOME }`.
 */
package org.ubada.xaiflows.ui.components.site

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Same light hero gradient HomePage.kt uses — kept as one named constant
 *  here rather than copy-pasted into every site screen. */
private val SITE_BACKGROUND_GRADIENT = Brush.linearGradient(
    listOf(Color(0xFFE0F2FE), Color.White, Color(0xFFEDE9FE))
)

private val ICON_BADGE_GRADIENT = Brush.linearGradient(
    listOf(Color(0xFF3B82F6), Color(0xFF6366F1))
)

private val TITLE_GRADIENT_START = Color(0xFF2563EB)
private val TITLE_GRADIENT_END   = Color(0xFF4F46E5)

@Composable
fun SitePageScaffold(
    title: String,
    icon: ImageVector,
    onBack: () -> Unit,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SITE_BACKGROUND_GRADIENT)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // ── Back to Home ─────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onBack() }
                .padding(vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = TITLE_GRADIENT_START,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Back to Home", color = TITLE_GRADIENT_START, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Icon badge + title ───────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(ICON_BADGE_GRADIENT, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TITLE_GRADIENT_END
            )
        }

        subtitle?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = it, color = Color(0xFF4B5563), fontSize = 14.sp, lineHeight = 20.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        content()

        Spacer(modifier = Modifier.height(40.dp))
    }
}
