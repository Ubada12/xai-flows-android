package com.example.xai_flows.ui.components.footer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xai_flows.ui.models.FooterSection

@Composable
fun FooterSectionComponent(section: FooterSection) {
    Column {
        Text(
            text = section.title,
            color = Color.White,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        section.links.forEach { link ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Icon(link.icon, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = link.name, color = Color(0xFF9CA3AF), fontSize = 12.sp)
            }
        }
    }
}
