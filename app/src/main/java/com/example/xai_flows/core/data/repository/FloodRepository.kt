package com.example.xai_flows.core.data.repository

import com.example.xai_flows.core.data.api.ApiService
import com.example.xai_flows.core.data.models.PredictFloodRequest
import okhttp3.MultipartBody

class FloodRepository(private val api: ApiService) {

    suspend fun getLatestImage() = api.getLatestS3Image()

    suspend fun reverseGeocode(lat: Double, lon: Double) =
        api.reverseGeocode(lat, lon)

    suspend fun predictFlood(imagePart: MultipartBody.Part, request: PredictFloodRequest) =
        api.predictFlood(imagePart, request)
}