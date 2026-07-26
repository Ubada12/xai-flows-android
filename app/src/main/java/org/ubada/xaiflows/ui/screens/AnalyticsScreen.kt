package org.ubada.xaiflows.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.ubada.xaiflows.R
import org.ubada.xaiflows.ui.components.analytics.*
import org.ubada.xaiflows.ui.models.ConclusionInsight
import org.ubada.xaiflows.ui.models.Metric
import org.ubada.xaiflows.ui.models.TechnicalDetails
import org.ubada.xaiflows.ui.components.common.FooterMobile

val conclusionInsights = listOf(
    ConclusionInsight(
        title = "Sustained Rainfall Impact",
        description = "3-day rainfall averages predict floods better than single-day data, showing risk builds over time.",
        recommendation = "Track cumulative rainfall for early warnings.",
        icon = Icons.Default.TrendingUp,
        iconTint = Color(0xFF3B82F6)
    ),
    ConclusionInsight(
        title = "Infrastructure Criticality",
        description = "Drainage blockages are key flood indicators, stressing the need for infrastructure upkeep.",
        recommendation = "Proactively monitor and maintain drainage systems.",
        icon = Icons.Default.WarningAmber,
        iconTint = Color(0xFFFACC15)
    ),
    ConclusionInsight(
        title = "Conservative Approach",
        description = "Model favors false positives over misses, prioritizing safety.",
        recommendation = "Keep this bias but improve precision with more data.",
        icon = Icons.Default.CheckCircle,
        iconTint = Color(0xFF22C55E)
    )
)

@Composable
fun AnalyticsScreen(
    /** Forwarded to FooterMobile for in-app section link navigation. */
    onNavigate: (String) -> Unit = {}
) {
    // Data (replace with real drawable resources)
    val analyticsData = listOf(
        Metric(
            title = "Confusion Matrix",
            image = R.drawable.confusion_matrix,
            category = "performance",
            description = stringResource(id = R.string.analytics_confusion_matrix),
            keyInsights = listOf(
                "578 True Negatives: Correct predictions",
                "233 True Positives: Correctly detected floods",
                "77 False Positives: Conservative predictions",
                "42 False Negatives: Missed flood events"
            ),
            technicalDetails = TechnicalDetails(accuracy = 0.87)
        ),
        Metric(
            title = "Model Performance Metrics",
            image = R.drawable.model_performance_metrics,
            category = "performance",
            description = stringResource(id = R.string.analytics_model_performance_metrics),
            keyInsights = listOf(
                "87% Accuracy",
                "75% Precision",
                "85% Recall",
                "80% F1 Score"
            ),
            technicalDetails = TechnicalDetails(
                accuracy = 0.87,
                precision = 0.75,
                recall = 0.85,
                f1Score = 0.8
            )
        ),
        Metric(
            title = "Precision-Recall Curve",
            image = R.drawable.precision_recall_curve,
            category = "visualization",
            description = stringResource(id = R.string.analytics_precision_recall_curve),
            keyInsights = listOf(
                "High precision (>80%) maintained until 80% recall",
                "Sharp precision drop in final 20% of cases",
                "Optimal threshold balances safety vs. false alarms",
                "Model excels at high-confidence predictions"
            )
        ),
        Metric(
            title = "ROC Curve Analysis",
            image = R.drawable.roc_curve,
            category = "visualization",
            description = stringResource(id = R.string.analytics_roc_curve_analysis),
            keyInsights = listOf(
                "AUC = 0.90: Excellent discriminative performance",
                "Strong true positive vs false positive balance",
                "Significantly outperforms random chance",
                "Robust across various threshold settings"
            ),
            technicalDetails = TechnicalDetails(auc = 0.90)
        ),
        Metric(
            title = "SHAP Feature Importance",
            image = R.drawable.shap_feature_importance,
            category = "interpretation",
            description = stringResource(id = R.string.analytics_shap_feature_importance),
            keyInsights = listOf(
                "3-day precipitation average: Primary flood predictor",
                "Drainage blockage: Critical infrastructure factor",
                "Single-day precipitation: Less predictive than sustained rainfall",
                "Multi-day patterns more reliable than isolated events"
            )
        )
    )
    var showImageModal by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<Int?>(null) }
    var selectedTitle by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFF9FAFB), Color(0xFFE0F2FE), Color(0xFFEDE9FE))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)     // Enables scroll
                .padding(0.dp),                 // Same padding as before
            verticalArrangement = Arrangement.spacedBy(24.dp) // Keep same spacing
        ) {
            // Header Section
            Column(
                Modifier.padding(
                    PaddingValues(
                        start = 24.dp,
                        end = 16.dp,
                        top = 24.dp
                    )
                )
            ) {
                AnalyticsHeader()
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Metrics Cards (loop over analyticsData)
            Column(
                Modifier.padding(horizontal = 24.dp)
            ) {
                analyticsData.forEach { metric ->
                    MetricCard(metric) { imageRes, title ->
                        selectedImage = imageRes
                        selectedTitle = title
                        showImageModal = true
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Conclusion Section
            Column(
                Modifier.padding(horizontal = 24.dp)
            ) {
                ConclusionSection(conclusionInsights)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Footer Section
            FooterMobile(onNavigate = onNavigate)
        }

        // Image Modal (overlay)
        if (showImageModal && selectedImage != null) {
            ImageModal(
                imageRes = selectedImage!!,
                title = selectedTitle,
                onDismiss = { showImageModal = false }
            )
        }
    }
}