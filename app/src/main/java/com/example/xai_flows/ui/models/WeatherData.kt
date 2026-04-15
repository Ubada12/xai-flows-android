package com.example.xai_flows.ui.models

data class WeatherData(
    val temp: Double,
    val appTemp: Double,
    val humidity: Int,
    val windSpeed: Double,
    val uv: Double,
    val pressure: Double,
    val visibility: Double,
    val weatherCondition: String,
    val precipitation: Int,
    val airState: String,
    val windDirection: String,
    val cloudCoverage: String,
    val timeOfDay: String
)
