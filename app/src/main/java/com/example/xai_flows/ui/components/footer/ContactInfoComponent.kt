package com.example.xai_flows.ui.components.footer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xai_flows.ui.models.COMPANY_INFO

@Composable
fun ContactInfoComponent() {
    Column {
        Text(
            text = "Contact Info",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = COMPANY_INFO.email, color = Color(0xFF9CA3AF), fontSize = 12.sp)
        Text(text = COMPANY_INFO.phone, color = Color(0xFF9CA3AF), fontSize = 12.sp)
        Text(text = COMPANY_INFO.address, color = Color(0xFF9CA3AF), fontSize = 12.sp)
    }
}
