package org.ubada.xaiflows.ui.components.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow

data class FeatureCard(val title: String, val description: String, val icon: String)

@Composable
fun FeaturesSection() {

    val featureList = listOf(
        FeatureCard(
            title = "AI-Driven Flood Predictions",
            description = stringResource(org.ubada.xaiflows.R.string.features_flood_predictions),
            icon = "🤖"
        ),
        FeatureCard(
            title = "Real-Time Monitoring",
            description = stringResource(org.ubada.xaiflows.R.string.features_real_time_monitoring),
            icon = "📡"
        ),
        FeatureCard(
            title = "Explainable AI",
            description = stringResource(org.ubada.xaiflows.R.string.features_explainable_ai),
            icon = "🔍"
        ),
        FeatureCard(
            title = "Flood Risk Alerts",
            description = stringResource(org.ubada.xaiflows.R.string.features_flood_risk_alerts),
            icon = "🚨"
        ),
        FeatureCard(
            title = "User-Friendly Interface",
            description = stringResource(org.ubada.xaiflows.R.string.features_user_friendly_interface),
            icon = "👥"
        ),
        FeatureCard(
            title = "Interactive Visualization Tools",
            description = stringResource(org.ubada.xaiflows.R.string.features_interactive_visualization_tools),
            icon = "📊"
        )
    )

    Column {
        SectionHeader(
            title = "System Features",
            subtitle = "Advanced AI technology for flood monitoring"
        )

        FlowRow(
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 8.dp,
            modifier = Modifier.padding(8.dp)
        ) {
            featureList.forEach { feature ->
                FeatureCardView(feature)
            }
        }
    }
}

@Composable
fun FeatureCardView(feature: FeatureCard) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = feature.icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = feature.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = feature.description, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}
