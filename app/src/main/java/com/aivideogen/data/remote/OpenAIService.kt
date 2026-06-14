package com.aivideogen.data.remote

import retrofit2.Response
import retrofit2.http.*

interface OpenAIService {

    @POST("v1/images/generations")
    @Headers("Content-Type: application/json")
    suspend fun generateImage(
        @Header("Authorization") apiKey: String,
        @Body request: DallERequest
    ): Response<DallEResponse>

    @POST("v1/images/variations")
    @Headers("Content-Type: application/json")
    suspend fun imageVariation(
        @Header("Authorization") apiKey: String,
        @Body request: DallEVariationRequest
    ): Response<DallEResponse>
}

data class DallERequest(
    val prompt: String,
    val model: String = "dall-e-3",
    val n: Int = 1,
    val size: String = "1024x1024",   // "1024x1024" | "1792x1024" | "1024x1792"
    val quality: String = "standard", // "standard" | "hd"
    val style: String = "vivid",      // "vivid" | "natural"
    val response_format: String = "b64_json"
)

data class DallEVariationRequest(
    val image: String,  // base64 encoded PNG
    val n: Int = 1,
    val size: String = "1024x1024",
    val response_format: String = "b64_json"
)

data class DallEResponse(
    val created: Long,
    val data: List<DallEImage>
)

data class DallEImage(
    val b64_json: String? = null,
    val url: String? = null,
    val revised_prompt: String? = null
)
