package com.aivideogen.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aivideogen.data.model.*
import com.aivideogen.data.repository.VideoRepository
import com.aivideogen.utils.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class VideoGeneratorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: VideoRepository
) : ViewModel() {

    // ── Projects list ─────────────────────────

    val allProjects: StateFlow<List<VideoProject>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completedProjects: StateFlow<List<VideoProject>> = repository.getCompletedProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Current project being built ───────────

    private val _currentProject = MutableStateFlow(VideoProject())
    val currentProject: StateFlow<VideoProject> = _currentProject.asStateFlow()

    // ── Generation state ──────────────────────

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    // ── Selected images ───────────────────────

    private val _selectedImagePaths = MutableStateFlow<List<String>>(emptyList())
    val selectedImagePaths: StateFlow<List<String>> = _selectedImagePaths.asStateFlow()

    // ── API keys (in-memory; persisted via DataStore in prod) ────

    private var stabilityApiKey: String = ""
    private var openAiApiKey: String = ""

    // ── Project editing ───────────────────────

    fun setPrompt(text: String) {
        _currentProject.value = _currentProject.value.copy(prompt = text)
    }

    fun setNegativePrompt(text: String) {
        _currentProject.value = _currentProject.value.copy(negativePrompt = text)
    }

    fun setStyle(style: VideoStyle) {
        _currentProject.value = _currentProject.value.copy(style = style)
    }

    fun setDuration(seconds: Int) {
        _currentProject.value = _currentProject.value.copy(duration = seconds)
    }

    fun setResolution(resolution: VideoResolution) {
        _currentProject.value = _currentProject.value.copy(resolution = resolution)
    }

    fun setAIProvider(provider: AIProvider) {
        _currentProject.value = _currentProject.value.copy(aiProvider = provider)
    }

    fun setMotionStrength(strength: Float) {
        _currentProject.value = _currentProject.value.copy(motionStrength = strength)
    }

    fun setCfgScale(scale: Float) {
        _currentProject.value = _currentProject.value.copy(cfgScale = scale)
    }

    fun setTitle(title: String) {
        _currentProject.value = _currentProject.value.copy(title = title)
    }

    fun setApiKeys(stability: String, openAi: String) {
        stabilityApiKey = stability
        openAiApiKey = openAi
    }

    // ── Image selection ───────────────────────

    fun addImagePath(path: String) {
        val current = _selectedImagePaths.value.toMutableList()
        if (!current.contains(path)) {
            current.add(path)
            _selectedImagePaths.value = current
            _currentProject.value = _currentProject.value.copy(imagePaths = current)
        }
    }

    fun removeImagePath(path: String) {
        val current = _selectedImagePaths.value.toMutableList()
        current.remove(path)
        _selectedImagePaths.value = current
        _currentProject.value = _currentProject.value.copy(imagePaths = current)
    }

    fun clearImages() {
        _selectedImagePaths.value = emptyList()
        _currentProject.value = _currentProject.value.copy(imagePaths = emptyList())
    }

    fun reorderImages(from: Int, to: Int) {
        val list = _selectedImagePaths.value.toMutableList()
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from)
            list.add(to, item)
            _selectedImagePaths.value = list
            _currentProject.value = _currentProject.value.copy(imagePaths = list)
        }
    }

    // ── Generation ────────────────────────────

    fun startGeneration() {
        val project = _currentProject.value

        if (project.prompt.isBlank() && project.imagePaths.isEmpty()) {
            _generationState.value = GenerationState.Error("Please add a prompt or select images")
            return
        }

        viewModelScope.launch {
            // Save project first to get ID
            val projectId = repository.saveProject(
                project.copy(status = GenerationStatus.PROCESSING, progress = 0)
            )

            val request = GenerationRequest(
                prompt = project.prompt,
                negativePrompt = project.negativePrompt,
                style = project.style,
                duration = project.duration,
                fps = project.fps,
                resolution = project.resolution,
                inputImages = project.imagePaths,
                aiProvider = project.aiProvider,
                motionStrength = project.motionStrength,
                cfgScale = project.cfgScale,
                seed = project.seedValue
            )

            repository.generateVideo(projectId, request, stabilityApiKey, openAiApiKey)
                .collect { state ->
                    _generationState.value = state
                    Timber.d("Generation state: $state")
                }
        }
    }

    fun cancelGeneration() {
        _generationState.value = GenerationState.Cancelled
    }

    fun resetState() {
        _generationState.value = GenerationState.Idle
        _currentProject.value = VideoProject()
        _selectedImagePaths.value = emptyList()
    }

    // ── Project management ────────────────────

    fun deleteProject(project: VideoProject) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }

    suspend fun getProjectById(id: Long): VideoProject? = repository.getProjectById(id)
}
