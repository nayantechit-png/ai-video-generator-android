package com.aivideogen.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.aivideogen.data.local.VideoProjectDao
import com.aivideogen.data.model.*
import com.aivideogen.data.remote.OpenAIService
import com.aivideogen.data.remote.StabilityAIService
import com.aivideogen.data.remote.TextPrompt
import com.aivideogen.data.remote.TextToImageRequest
import com.aivideogen.data.remote.DallERequest
import com.aivideogen.utils.FileUtils
import com.aivideogen.utils.VideoUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoProjectDao: VideoProjectDao,
    private val stabilityAIService: StabilityAIService,
    private val openAIService: OpenAIService
) {

    // ── Database operations ──────────────────

    fun getAllProjects(): Flow<List<VideoProject>> = videoProjectDao.getAllProjects()

    fun getCompletedProjects(): Flow<List<VideoProject>> = videoProjectDao.getCompletedProjects()

    suspend fun getProjectById(id: Long): VideoProject? = videoProjectDao.getProjectById(id)

    suspend fun saveProject(project: VideoProject): Long = videoProjectDao.insertProject(project)

    suspend fun updateProject(project: VideoProject) = videoProjectDao.updateProject(project)

    suspend fun deleteProject(project: VideoProject) {
        project.outputVideoPath?.let { File(it).delete() }
        project.thumbnailPath?.let { File(it).delete() }
        videoProjectDao.deleteProject(project)
    }

    // ── Generation ────────────────────────────

    /**
     * Main generation flow – emits [GenerationState] updates.
     * Handles both image-to-video and text-to-video paths.
     */
    fun generateVideo(
        projectId: Long,
        request: GenerationRequest,
        stabilityApiKey: String,
        openAiApiKey: String
    ): Flow<GenerationState> = flow {
        emit(GenerationState.Progress(5, "Preparing generation…"))

        try {
            val imageFiles: List<File> = if (request.inputImages.isNotEmpty()) {
                // User supplied images → use them directly
                request.inputImages.map { File(it) }.filter { it.exists() }
            } else {
                // No images → generate from prompt first
                emit(GenerationState.Progress(15, "Generating image from prompt…"))
                val generated = generateImageFromPrompt(request, stabilityApiKey, openAiApiKey)
                listOfNotNull(generated)
            }

            if (imageFiles.isEmpty()) {
                emit(GenerationState.Error("No valid source images available"))
                return@flow
            }

            emit(GenerationState.Progress(30, "Sending to AI for animation…"))

            val generationId = when (request.aiProvider) {
                AIProvider.STABILITY_AI -> startStabilityVideoGeneration(
                    imageFiles.first(), request, stabilityApiKey
                )
                AIProvider.OPENAI -> startOpenAIVideoGeneration(
                    imageFiles, request, openAiApiKey
                )
                AIProvider.LOCAL -> startLocalVideoGeneration(imageFiles, request)
            }

            emit(GenerationState.Progress(40, "AI processing video… (this may take 1-3 min)"))

            // Poll for result
            val videoPath = pollForVideo(
                generationId = generationId,
                projectId = projectId,
                request = request,
                stabilityApiKey = stabilityApiKey
            ) { progress ->
                emit(GenerationState.Progress(progress, "Generating video frames…"))
            }

            emit(GenerationState.Progress(90, "Saving video…"))

            val thumbPath = VideoUtils.extractThumbnail(context, videoPath)

            videoProjectDao.markCompleted(projectId, videoPath, thumbPath)

            emit(GenerationState.Success(GenerationResult(
                success = true,
                videoPath = videoPath,
                thumbnailPath = thumbPath,
                generationId = generationId
            )))

        } catch (e: Exception) {
            Timber.e(e, "Video generation failed")
            val msg = e.message ?: "Unknown error"
            videoProjectDao.markFailed(projectId, msg)
            emit(GenerationState.Error(msg))
        }
    }

    // ── Stability AI ─────────────────────────

    private suspend fun startStabilityVideoGeneration(
        imageFile: File,
        request: GenerationRequest,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        val imgPart = MultipartBody.Part.createFormData(
            "image", imageFile.name,
            imageFile.asRequestBody("image/*".toMediaType())
        )
        val seedBody = request.seed.toString().toRequestBody("text/plain".toMediaType())
        val cfgBody  = request.cfgScale.toString().toRequestBody("text/plain".toMediaType())
        val motionId = (request.motionStrength * 255).toInt().coerceIn(1, 255)
        val motionBody = motionId.toString().toRequestBody("text/plain".toMediaType())

        val response = stabilityAIService.imageToVideo(
            apiKey = "Bearer $apiKey",
            image = imgPart,
            seed = seedBody,
            cfgScale = cfgBody,
            motionBucketId = motionBody
        )

        if (response.isSuccessful) {
            response.body()?.id ?: throw Exception("No generation ID returned")
        } else {
            throw Exception("Stability AI error ${response.code()}: ${response.errorBody()?.string()}")
        }
    }

    private suspend fun pollForVideo(
        generationId: String,
        projectId: Long,
        request: GenerationRequest,
        stabilityApiKey: String,
        onProgress: suspend (Int) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val outputDir = FileUtils.getVideoOutputDir(context)
        val outputFile = File(outputDir, "video_${System.currentTimeMillis()}.mp4")

        if (request.aiProvider == AIProvider.LOCAL) {
            // Local generation already saved the file
            return@withContext generationId
        }

        var attempts = 0
        val maxAttempts = 60  // 5 minutes max

        while (attempts < maxAttempts) {
            delay(5_000) // poll every 5 s
            attempts++

            val progress = 40 + (attempts * 50 / maxAttempts).coerceAtMost(50)
            onProgress(progress)

            val response = stabilityAIService.getVideoResult(
                apiKey = "Bearer $stabilityApiKey",
                generationId = generationId
            )

            when {
                response.isSuccessful -> {
                    response.body()?.let { body ->
                        outputFile.outputStream().use { out ->
                            body.byteStream().copyTo(out)
                        }
                        return@withContext outputFile.absolutePath
                    }
                }
                response.code() == 202 -> {
                    // Still processing – keep polling
                    Timber.d("Still processing, attempt $attempts/$maxAttempts")
                }
                else -> {
                    throw Exception("Poll failed ${response.code()}: ${response.errorBody()?.string()}")
                }
            }
        }
        throw Exception("Generation timed out after ${maxAttempts * 5} seconds")
    }

    // ── OpenAI path ───────────────────────────

    private suspend fun startOpenAIVideoGeneration(
        imageFiles: List<File>,
        request: GenerationRequest,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        // OpenAI doesn't have a direct video API; we generate a series of images
        // and stitch them into a video locally.
        val frames = mutableListOf<File>()
        val frameCount = request.duration * request.fps

        for (i in 0 until minOf(frameCount, 10)) {
            val framePrompt = "${request.prompt}, frame ${i + 1} of $frameCount, smooth motion"
            val dresp = openAIService.generateImage(
                apiKey = "Bearer $apiKey",
                request = DallERequest(
                    prompt = framePrompt,
                    size = "${request.resolution.width}x${request.resolution.height}"
                        .let { if (it.contains("x")) it else "1024x1024" }
                )
            )
            if (dresp.isSuccessful) {
                val b64 = dresp.body()?.data?.firstOrNull()?.b64_json ?: continue
                val frameFile = FileUtils.saveBase64Image(context, b64, "frame_$i")
                frames.add(frameFile)
            }
        }

        // Stitch frames into video
        val outputPath = VideoUtils.framesToVideo(context, frames, request.fps)
        frames.forEach { it.delete() }
        outputPath   // return path as "generationId" for the local provider
    }

    // ── Local / offline generation ────────────

    private suspend fun startLocalVideoGeneration(
        imageFiles: List<File>,
        request: GenerationRequest
    ): String = withContext(Dispatchers.IO) {
        // Simple slide-show with Ken Burns effect as offline fallback
        VideoUtils.createSlideshow(context, imageFiles, request.duration, request.fps)
    }

    // ── Image generation helpers ───────────────

    private suspend fun generateImageFromPrompt(
        request: GenerationRequest,
        stabilityApiKey: String,
        openAiApiKey: String
    ): File? = withContext(Dispatchers.IO) {
        when (request.aiProvider) {
            AIProvider.STABILITY_AI -> {
                val textPrompts = buildList {
                    add(TextPrompt(request.prompt, 1.0f))
                    if (request.negativePrompt.isNotBlank()) {
                        add(TextPrompt(request.negativePrompt, -1.0f))
                    }
                }
                val resp = stabilityAIService.textToImage(
                    apiKey = "Bearer $stabilityApiKey",
                    request = TextToImageRequest(
                        text_prompts = textPrompts,
                        height = request.resolution.height,
                        width = request.resolution.width,
                        style_preset = request.style.apiValue.takeIf { it.isNotBlank() }
                    )
                )
                if (resp.isSuccessful) {
                    val b64 = resp.body()?.artifacts?.firstOrNull()?.base64 ?: return@withContext null
                    FileUtils.saveBase64Image(context, b64, "generated_src")
                } else null
            }
            AIProvider.OPENAI -> {
                val resp = openAIService.generateImage(
                    apiKey = "Bearer $openAiApiKey",
                    request = DallERequest(
                        prompt = request.prompt,
                        size = "1024x1024"
                    )
                )
                if (resp.isSuccessful) {
                    val b64 = resp.body()?.data?.firstOrNull()?.b64_json ?: return@withContext null
                    FileUtils.saveBase64Image(context, b64, "generated_src")
                } else null
            }
            AIProvider.LOCAL -> null
        }
    }
}
