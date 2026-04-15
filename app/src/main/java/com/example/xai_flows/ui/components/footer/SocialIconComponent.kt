package com.example.xai_flows.ui.components.footer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.xai_flows.ui.models.SocialLink

@Composable
fun SocialIconComponent(social: SocialLink) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(social.color.copy(alpha = 0.15f), shape = RoundedCornerShape(50))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(social.icon, contentDescription = social.name, tint = social.color, modifier = Modifier.size(20.dp))
    }
}
