/**
 * PredictFloodResponse.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Kotlin data classes that mirror the unified backend response from
 * POST /api/v1/predict-flood/
 *
 * Backend shape (FastAPI / Pydantic v2):
 * {
 *   "prediction":            { "flood_risk": str, "reason": str },
 *   "location":             { "latitude": float, "longitude": float,
 *                             "address": str, "city": str },
 *   "weather":              { "temp": float, "app_temp": float,
 *                             "humidity": float, "wind_speed": float,
 *                             "wind_dir": int, "uv": float,
 *                             "pressure": float, "visibility": float,
 *                             "precipitation": float, "condition": str,
 *                             "clouds": int, "dewpt": float },
 *   "weather_shap_value":   [ { "feature": str, "value": float } ] | null,
 *   "drain_blockage":       int | null,      // 0=Full 1=None 2=Partial
 *   "drain_blockage_prob":  float | null,
 *   "drain_blockage_shap_value": any | null,
 *   "alert_sent":           bool
 * }
 *
 * Gson will map JSON snake_case names automatically when field names match.
 * Any field absent in the JSON is initialised to its Kotlin default.
 */
package org.ubada.xaiflows.core.data.models

// ─── Top-level response ───────────────────────────────────────────────────────

data class PredictFloodResponse(
    /** XGBoost + heuristic combined flood risk assessment. */
    val prediction: Prediction,

    /** Reverse-geocoded coordinates returned by the backend. */
    val location: Location,

    /** Cleaned, normalised weather snapshot used for inference. */
    val weather: WeatherInfo,

    /**
     * SHAP explanations for each weather feature.
     * Null when the XGBoost model did not produce SHAP values
     * (e.g., fallback heuristic path).
     */
    val weather_shap_value: List<ShapValue>?,

    /**
     * VGG16 drain-blockage classification result.
     * 0 = Full blockage, 1 = No blockage, 2 = Partial blockage.
     * Null when no image was analysed.
     */
    val drain_blockage: Int?,

    /** Confidence probability of the drain-blockage prediction (0–1). */
    val drain_blockage_prob: Double?,

    /** SHAP values for the drain-blockage model (reserved for future use). */
    val drain_blockage_shap_value: Any?,

    /** True when a flood-alert email was dispatched during this request. */
    val alert_sent: Boolean = false
)

// ─── Nested models ────────────────────────────────────────────────────────────

/** XGBoost / heuristic risk verdict. */
data class Prediction(
    /** "High", "Moderate", or "Low" */
    val flood_risk: String,
    /** Human-readable explanation of the risk factors. */
    val reason: String
)

/** Reverse-geocoded location attached to the prediction. */
data class Location(
    val latitude: Double,
    val longitude: Double,
    /** Street-level address string (may be empty if geocoding fails). */
    val address: String,
    /** City name, e.g. "Mumbai". */
    val city: String
)

/**
 * Weather snapshot from Weatherbit API, cleaned and normalised by the backend.
 * Field names intentionally match the JSON keys so Gson can map them directly.
 */
data class WeatherInfo(
    /** Air temperature (°C). */
    val temp: Double,
    /** Apparent ("feels-like") temperature (°C). */
    val app_temp: Double,
    /** Relative humidity (%). */
    val humidity: Double,
    /** Wind speed (m/s). */
    val wind_speed: Double,
    /** Wind direction (degrees, 0–360). */
    val wind_dir: Int,
    /** UV index. */
    val uv: Double,
    /** Atmospheric pressure (mb). */
    val pressure: Double,
    /** Visibility (km). */
    val visibility: Double,
    /** Precipitation in the last hour (mm). */
    val precipitation: Double,
    /** Weather condition description, e.g. "Light rain". */
    val condition: String,
    /** Cloud coverage (%). */
    val clouds: Int,
    /** Dew-point temperature (°C), used to derive air-quality state. */
    val dewpt: Double
)

/** Single SHAP feature-importance entry. */
data class ShapValue(
    /** Weather feature name, e.g. "precipitation", "humidity". */
    val feature: String,
    /** SHAP impact value (positive = increases flood risk, negative = decreases). */
    val value: Double
)
