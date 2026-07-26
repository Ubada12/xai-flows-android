/**
 * ApiService.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Retrofit interface declaring all active XAI-FLOWS backend endpoints.
 *
 * Removed endpoint:
 *   GET /api/v1/reverse-geocode — the backend now performs geocoding
 *   internally and returns location data inside PredictFloodResponse.location.
 */
package org.ubada.xaiflows.core.data.api

import org.ubada.xaiflows.core.data.models.GetLatestImageResponse
import org.ubada.xaiflows.core.data.models.PredictFloodRequest
import org.ubada.xaiflows.core.data.models.PredictFloodResponse
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    /**
     * Fetches the latest S3-stored drain image as a base64-encoded string.
     * Called by the real-time monitoring loop every 20 seconds.
     */
    @GET("api/v1/get-latest-s3-image")
    suspend fun getLatestS3Image(): GetLatestImageResponse

    /**
     * Submits a drain image for full flood-risk analysis.
     *
     * The backend pipeline:
     * 1. VGG16 → drain blockage classification (Full / None / Partial)
     * 2. Weatherbit API → current weather at [lat, lon]
     * 3. XGBoost → flood risk prediction with SHAP explanations
     * 4. Reverse geocoding → human-readable address / city
     * 5. If risk is High/Moderate → sends email alert via MSG91
     *
     * @param image  JPEG/PNG multipart part (field name must be "image")
     * @param request JSON body containing lat/lon coordinates
     */
    @Multipart
    @POST("api/v1/predict-flood/")
    suspend fun predictFlood(
        @Part image: MultipartBody.Part,
        @Part("request") request: PredictFloodRequest
    ): PredictFloodResponse
}
