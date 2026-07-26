/**
 * ContactInfoComponent.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Displays company contact details as tappable rows:
 *  • Email  → opens mail app  (ACTION_SENDTO mailto:)
 *  • Phone  → opens dialler   (ACTION_DIAL tel:)
 *  • Address → opens Maps     (ACTION_VIEW geo:)
 */
package org.ubada.xaiflows.ui.components.footer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ubada.xaiflows.core.utils.IntentUtils
import org.ubada.xaiflows.ui.models.COMPANY_INFO

@Composable
fun ContactInfoComponent() {
    val context = LocalContext.current

    Column {
        Text(
            text       = "Contact Info",
            color      = Color.White,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Email row
        ContactRow(
            icon    = Icons.Filled.Email,
            label   = COMPANY_INFO.email,
            onClick = { IntentUtils.openEmail(context, COMPANY_INFO.email) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Phone row
        ContactRow(
            icon    = Icons.Filled.Phone,
            label   = COMPANY_INFO.phone,
            onClick = { IntentUtils.openPhone(context, COMPANY_INFO.phone) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Address row
        ContactRow(
            icon    = Icons.Filled.LocationOn,
            label   = COMPANY_INFO.address,
            onClick = { IntentUtils.openMaps(context, COMPANY_INFO.address) }
        )
    }
}

// ─── Private helper ───────────────────────────────────────────────────────────

@Composable
private fun ContactRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(color = Color(0xFF60A5FA)),
                role              = Role.Button,
                onClickLabel      = label
            ) { onClick() }
            .padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = Color(0xFF60A5FA),
            modifier           = Modifier.size(14.dp).padding(top = 1.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text     = label,
            color    = Color(0xFF9CA3AF),
            fontSize = 12.sp
        )
    }
}
