/**
 * ShapData.kt  (UI model)
 * ─────────────────────────────────────────────────────────────────────────────
 * Presentation-layer SHAP entry consumed by ShapChart.
 * Mapped from ShapValue (network model) in PredictionScreen.
 */
package org.ubada.xaiflows.ui.models

data class ShapData(
    /** Weather feature name, e.g. "precipitation", "humidity". */
    val feature: String,
    /**
     * SHAP impact value as Float for chart rendering.
     * Positive = increases flood risk, negative = decreases risk.
     */
    val value: Float
)
