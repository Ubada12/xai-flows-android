/**
 * WeatherData.kt  (UI model)
 * ─────────────────────────────────────────────────────────────────────────────
 * Presentation-layer weather snapshot consumed by WeatherCard.
 *
 * Fields are mapped from WeatherInfo (network model) in PredictionScreen:
 *   humidity    ← WeatherInfo.humidity    (backend returns float, not int)
 *   precipitation ← WeatherInfo.precipitation (mm, float)
 *
 * Previously humidity and precipitation were typed as Int, causing a
 * truncation of decimal values returned by the Weatherbit API.
 */
package org.ubada.xaiflows.ui.models

data class WeatherData(
    /** Air temperature (°C). */
    val temp: Double,
    /** Apparent ("feels-like") temperature (°C). */
    val appTemp: Double,
    /** Relative humidity (%). Float — Weatherbit returns fractional values. */
    val humidity: Double,
    /** Wind speed (m/s). */
    val windSpeed: Double,
    /** UV index. */
    val uv: Double,
    /** Atmospheric pressure (mb). */
    val pressure: Double,
    /** Visibility (km). */
    val visibility: Double,
    /** Weather condition label, e.g. "Light rain", "Clear sky". */
    val weatherCondition: String,
    /** Precipitation in the last hour (mm). Float — can be 0.0. */
    val precipitation: Double,
    /** Derived air-quality string, e.g. "Humid", "Dry". */
    val airState: String,
    /** Derived compass direction string, e.g. "Northeast". */
    val windDirection: String,
    /** Derived cloud description, e.g. "Partly Cloudy". */
    val cloudCoverage: String,
    /** Derived time-of-day label, e.g. "Morning", "Evening". */
    val timeOfDay: String
)
