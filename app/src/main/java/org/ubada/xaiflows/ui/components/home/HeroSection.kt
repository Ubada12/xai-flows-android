package org.ubada.xaiflows.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import org.ubada.xaiflows.R

@Composable
fun HeroSection(onExploreClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "XAI-FLOWS",
            style = TextStyle(
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF2563EB), Color(0xFF4F46E5), Color(0xFF9333EA))
                )
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Intelligent Flood Prediction & Monitoring",
            fontSize = 20.sp,
            color = Color(0xFF374151),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.hero_desc),
            fontSize = 16.sp,
            color = Color(0xFF4B5563),
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.hero_tagline),
            fontSize = 16.sp,
            color = Color(0xFF1D4ED8),
            fontStyle = FontStyle.Italic,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0F2FE), shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onExploreClick,
            modifier = Modifier
                .height(50.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Text(text = "Explore Our Platform", color = Color.White, fontSize = 18.sp)
        }
    }
}
