/**
 * SocialIconComponent.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Clickable social-media icon chip. Tapping opens the social profile URL in
 * the device's default browser via [IntentUtils.openUrl].
 */
package com.example.xai_flows.ui.components.footer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.xai_flows.core.utils.IntentUtils
import com.example.xai_flows.ui.models.SocialLink

@Composable
fun SocialIconComponent(social: SocialLink) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(social.color.copy(alpha = 0.15f), shape = RoundedCornerShape(50))
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(bounded = false, radius = 18.dp, color = social.color),
                role              = Role.Button,
                onClickLabel      = "Open ${social.name}"
            ) {
                IntentUtils.openUrl(context, social.href)
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = social.icon,
            contentDescription = social.name,
            tint               = social.color,
            modifier           = Modifier.size(20.dp)
        )
    }
}
