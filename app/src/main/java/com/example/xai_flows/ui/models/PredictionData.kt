package com.example.xai_flows.ui.models

data class PredictionData(
    val floodRisk: String,        // Risk Level (e.g., "High", "Low")
    val reason: String,           // AI explanation
    val drainBlockageProb: String, // Probability (e.g., "75%")
    val drainBlockage: Int,         // Status (0=Flood, 1=No Flood, 2=Semi Flood)
    val city: String,       // new
    val address: String
)
