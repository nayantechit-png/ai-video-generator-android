package com.aivideogen.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

// ─────────────────────────────────────────────
//  Stability AI REST client
// ─────────────────────────────────────────────
interface StabilityAIService {

    /** Image-to-video: upload a source image, get a generation ID */
    @Multipart
    @POST("v2beta/image-to-video")
    suspend fun imageToVideo(
        @Header("Authorization") apiKey: String,
        @Part image: MultipartBody.Part,
        @Part("seed") seed: RequestBody,
        @Part("cfg_scale") cfgScale: RequestBody,
        @Part("motion_bucket_id") motionBucketId: RequestBody
    ): Response<StartVideoResponse>

    /** Poll for the generated video */
    @GET("v2beta/image-to-video/result/{generationId}")
    suspend fun getVideoResult(
        @Header("Authorization") apiKey: String,
        @Header("Accept") accept: String = "video/*",
        @Path("generationId") generationId: String
    ): Response<ResponseBody>

    /** Text-to-image (for prompt-only generation then animate) */
    @POST("v1/generation/{engine_id}/text-to-image")
    @Headers("Content-Type: application/json")
    suspend fun textToImage(
        @Header("Authorization") apiKey: String,
        @Path("engine_id") engineId: String = "stable-diffusion-xl-1024-v1-0",
        @Body request: TextToImageRequest
    ): Response<TextToImageResponse>
}

// ── Request / Response DTOs ──────────────────

data class StartVideoResponse(
    val id: String
)

data class TextToImageRequest(
    val text_prompts: List<TextPrompt>,
    val cfg_scale: Float = 7f,
    val height: Int = 1024,
    val width: Int = 1024,
    val samples: Int = 1,
    val steps: Int = 30,
    val style_preset: String? = null
)

data class TextPrompt(
    val text: String,
    val weight: Float = 1.0f
)

data class TextToImageResponse(
    val artifacts: List<ImageArtifact>
)

data class ImageArtifact(
    val base64: String,
    val finishReason: String,
    val seed: Long
)
