package com.example.xai_flows.ui.components.footer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xai_flows.R
import com.example.xai_flows.ui.models.COMPANY_INFO

@Preview
@Composable
fun CompanyInfoComponent() {
    var email by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // --- Logo + Company Name ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.logo), // Use your file name here
                contentDescription = "Logo",
                modifier = Modifier
                    .size(58.dp)
                    .scale(1.1f) // mimic hover scale
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = COMPANY_INFO.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF60A5FA)
                )
                Text(
                    text = "Flood Prediction System",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Description ---
        Text(
            text = COMPANY_INFO.description,
            color = Color(0xFF9CA3AF),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Stay Updated Section ---
        Text(
            text = "Stay Updated",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text(
                    text = "Enter your email",
                    fontSize = 14.sp,           // <-- control font size
                    color = Color(0xFF9CA3AF),  // optional custom color
                    fontWeight = FontWeight.Normal
                )},
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(Color(0xFF1F2937), shape = RoundedCornerShape(8.dp)),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF3B82F6),
                    unfocusedIndicatorColor = Color(0xFF374151),
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color(0xFF9CA3AF),
                    unfocusedPlaceholderColor = Color(0xFF9CA3AF)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Gradient Button
            Button(
                onClick = { /* TODO: Handle subscription */ },
                modifier = Modifier
                    .height(26.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF3B82F6), Color(0xFF6366F1))
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("Subscribe", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Get the latest updates on flood predictions and system improvements.",
            color = Color(0xFF9CA3AF),
            fontSize = 10.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
