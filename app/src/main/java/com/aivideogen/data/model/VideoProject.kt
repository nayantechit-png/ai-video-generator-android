package com.aivideogen.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.aivideogen.data.local.Converters
import kotlinx.parcelize.Parcelize

@Entity(tableName = "video_projects")
@TypeConverters(Converters::class)
@Parcelize
data class VideoProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val prompt: String = "",
    val negativePrompt: String = "",
    val style: VideoStyle = VideoStyle.CINEMATIC,
    val duration: Int = 5,
    val fps: Int = 24,
    val resolution: VideoResolution = VideoResolution.HD_720,
    val imagePaths: List<String> = emptyList(),
    val outputVideoPath: String? = null,
    val thumbnailPath: String? = null,
    val status: GenerationStatus = GenerationStatus.PENDING,
    val progress: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val aiProvider: AIProvider = AIProvider.STABILITY_AI,
    val seedValue: Long = -1L,
    val motionStrength: Float = 0.7f,
    val cfgScale: Float = 7.0f
) : Parcelable

enum class VideoStyle(val displayName: String, val apiValue: String) {
    CINEMATIC("Cinematic", "cinematic"),
    ANIME("Anime", "anime"),
    REALISTIC("Realistic", "realistic"),
    DIGITAL_ART("Digital Art", "digital-art"),
    PHOTOGRAPHIC("Photographic", "photographic"),
    FANTASY("Fantasy", "fantasy"),
    CYBERPUNK("Cyberpunk", "cyberpunk"),
    WATERCOLOR("Watercolor", "watercolor"),
    OIL_PAINTING("Oil Painting", "oil-painting"),
    ABSTRACT("Abstract", "abstract"),
    NONE("None (Auto)", "")
}

enum class VideoResolution(val displayName: String, val width: Int, val height: Int) {
    SD_480("480p SD", 854, 480),
    HD_720("720p HD", 1280, 720),
    FHD_1080("1080p FHD", 1920, 1080),
    SQUARE("Square 1:1", 1024, 1024),
    PORTRAIT("Portrait 9:16", 576, 1024),
    LANDSCAPE("Landscape 16:9", 1024, 576)
}

enum class GenerationStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
}

enum class AIProvider(val displayName: String) {
    STABILITY_AI("Stability AI"),
    OPENAI("OpenAI DALL-E"),
    LOCAL("Local Offline")
}
