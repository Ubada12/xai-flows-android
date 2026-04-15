package com.example.xai_flows.core.data.models

data class PredictFloodResponse(
    val image: String,
    val longitude: Double,
    val latitude: Double,
    val prediction: Prediction,                 // nested
    val weather_data: WeatherData,              // nested
    val weather_prediction: WeatherPrediction,  // nested
    val weather_metadata: WeatherMetadata,      // nested
    val weather_shap_value: List<ShapValue>,    // list of feature impacts
    val drain_blockage: Int,
    val drain_blockage_prob: Double,
    val drain_blockage_shape_value: Any?        // null or future shape values
)

data class Prediction(
    val flood_risk: String,
    val reason: String
)

data class WeatherData(
    val app_temp: Double,
    val clouds: Int,
    val dewpt: Double,
    val dhi: Double,
    val dni: Double,
    val elev_angle: Double,
    val ghi: Double,
    val pres: Double,
    val rh: Int,
    val slp: Double,
    val solar_rad: Double,
    val temp: Double,
    val uv: Double,
    val vis: Int,
    val wind_dir: Int,
    val wind_spd: Double,
    val hour: Int,
    val month: Int
)

data class WeatherPrediction(
    val weather: String,
    val precip: Int
)

data class WeatherMetadata(
    val timezone: String,
    val temp: Double,
    val sources: List<String>,
    val country_code: String,
    val city_name: String
)

data class ShapValue(
    val feature: String,
    val value: Double
)
