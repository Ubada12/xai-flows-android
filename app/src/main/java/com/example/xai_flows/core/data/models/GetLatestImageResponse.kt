/**
 * GetLatestImageResponse.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Response model for GET /api/v1/get-latest-s3-image
 *
 * The backend fetches the most recent drain image from the S3 bucket and
 * returns it as a raw base64-encoded JPEG string (no data URI prefix).
 */
package com.example.xai_flows.core.data.models

data class GetLatestImageResponse(
    /** Raw base64-encoded JPEG. Decode with android.util.Base64.decode(). */
    val imageBase64: String
)
