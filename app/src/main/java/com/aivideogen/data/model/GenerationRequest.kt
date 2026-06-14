package com.aivideogen.data.model

data class GenerationRequest(
    val prompt: String,
    val negativePrompt: String = "",
    val style: VideoStyle = VideoStyle.NONE,
    val duration: Int = 5,
    val fps: Int = 24,
    val resolution: VideoResolution = VideoResolution.HD_720,
    val inputImages: List<String> = emptyList(),
    val aiProvider: AIProvider = AIProvider.STABILITY_AI,
    val motionStrength: Float = 0.7f,
    val cfgScale: Float = 7.0f,
    val seed: Long = -1L
)

data class GenerationResult(
    val success: Boolean,
    val videoPath: String? = null,
    val thumbnailPath: String? = null,
    val errorMessage: String? = null,
    val generationId: String? = null
)

sealed class GenerationState {
    object Idle : GenerationState()
    data class Progress(val percent: Int, val message: String) : GenerationState()
    data class Success(val result: GenerationResult) : GenerationState()
    data class Error(val message: String) : GenerationState()
    object Cancelled : GenerationState()
}
