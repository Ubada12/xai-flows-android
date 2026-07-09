/**
 * PredictFloodRequest.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Multipart JSON body for POST /api/v1/predict-flood/
 *
 * Sent as the "request" part alongside the "image" file part.
 * The backend uses lat/lon to:
 *   1. Fetch weather data from Weatherbit API
 *   2. Reverse-geocode to city + address
 */
package com.example.xai_flows.core.data.models

data class PredictFloodRequest(
    /** Longitude (decimal degrees, WGS84). */
    val lon: Double,
    /** Latitude (decimal degrees, WGS84). */
    val lat: Double
)
