/**
 * FloodRepository.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Repository layer between the ViewModel and the Retrofit ApiService.
 *
 * Keeps the ViewModel clean by owning all network call plumbing.
 * Reverse-geocode has been removed — the backend now returns
 * location data directly inside PredictFloodResponse.location.
 */
package com.example.xai_flows.core.data.repository

import com.example.xai_flows.core.data.api.ApiService
import com.example.xai_flows.core.data.models.PredictFloodRequest
import okhttp3.MultipartBody

class FloodRepository(private val api: ApiService) {

    /** Fetch the latest drain image from S3 (base64). */
    suspend fun getLatestImage() = api.getLatestS3Image()

    /**
     * Run the full flood-prediction pipeline.
     *
     * @param imagePart  Multipart JPEG/PNG from camera or S3
     * @param request    Lat/lon for weather lookup and geocoding
     */
    suspend fun predictFlood(
        imagePart: MultipartBody.Part,
        request: PredictFloodRequest
    ) = api.predictFlood(imagePart, request)
}
