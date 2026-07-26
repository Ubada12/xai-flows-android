/**
 * PredictionData.kt  (UI model)
 * ─────────────────────────────────────────────────────────────────────────────
 * Presentation-layer model consumed by PredictionCard.
 * Mapped from PredictFloodResponse in PredictionScreen.
 */
package org.ubada.xaiflows.ui.models

data class PredictionData(
    /** "High", "Moderate", or "Low" flood risk. */
    val floodRisk: String,
    /** Human-readable AI explanation of the risk factors. */
    val reason: String,
    /**
     * Formatted blockage probability percentage string, e.g. "75.40".
     * When drain_blockage == 1 (No blockage), probability is inverted
     * (1 - prob) so the UI always shows the chance of an issue.
     */
    val drainBlockageProb: String,
    /** 0 = Full blockage, 1 = No blockage, 2 = Partial blockage. */
    val drainBlockage: Int,
    /** City name from backend reverse geocoding, e.g. "Mumbai". */
    val city: String,
    /** Street-level address from backend reverse geocoding. */
    val address: String
)
