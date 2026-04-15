package com.example.xai_flows.core.data.api

import com.example.xai_flows.core.data.models.GetLatestImageResponse
import com.example.xai_flows.core.data.models.PredictFloodRequest
import com.example.xai_flows.core.data.models.PredictFloodResponse
import com.example.xai_flows.core.data.models.ReverseGeocodeResponse
import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    @GET("api/v1/get-latest-s3-image")
    suspend fun getLatestS3Image(): GetLatestImageResponse

    @GET("api/v1/reverse-geocode")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): ReverseGeocodeResponse

    @Multipart
    @POST("api/v1/predict-flood/")
    suspend fun predictFlood(
        @Part image: MultipartBody.Part,
        @Part("request") request: PredictFloodRequest
    ): PredictFloodResponse
}