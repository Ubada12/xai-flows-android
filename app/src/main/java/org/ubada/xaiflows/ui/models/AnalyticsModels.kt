package org.ubada.xaiflows.ui.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class Metric(
    val title: String,
    val image: Int, // Drawable resource ID
    val category: String,
    val description: String,
    val keyInsights: List<String>,
    val technicalDetails: TechnicalDetails? = null
)

data class TechnicalDetails(
    val accuracy: Double? = null,
    val precision: Double? = null,
    val recall: Double? = null,
    val f1Score: Double? = null,
    val auc: Double? = null
)

data class ConclusionInsight(
    val title: String,
    val description: String,
    val recommendation: String,
    val icon: ImageVector,
    val iconTint: Color
)
