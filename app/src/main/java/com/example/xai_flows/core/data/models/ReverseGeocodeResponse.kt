/**
 * ReverseGeocodeResponse.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * DEPRECATED — This response model is no longer used.
 * The backend now performs reverse geocoding internally and returns location
 * data inside PredictFloodResponse.location (city + address).
 *
 * This file is kept to avoid breaking any external references during the
 * transition period. It can be safely deleted once all usages are confirmed
 * removed.
 */
package com.example.xai_flows.core.data.models

@Deprecated(
    message = "Reverse geocoding is now handled server-side. " +
        "Use PredictFloodResponse.location.city and .address instead.",
    level = DeprecationLevel.ERROR
)
data class ReverseGeocodeResponse(
    val city: String,
    val address: String
)
