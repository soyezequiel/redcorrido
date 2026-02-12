package com.routerecorder.feature.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routerecorder.core.domain.model.VideoConfig
import com.routerecorder.core.domain.model.VideoRenderPhase
import com.routerecorder.core.domain.model.VideoRenderProgress
import com.routerecorder.core.domain.repository.RoutePhotoRepository
import com.routerecorder.core.domain.repository.RouteRepository
import com.routerecorder.core.domain.repository.TrackPointRepository
import com.routerecorder.service.videorender.VideoGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val trackPointRepository: TrackPointRepository,
    private val routePhotoRepository: RoutePhotoRepository,
    private val routeRepository: RouteRepository,
    private val videoGenerator: VideoGenerator
) : ViewModel() {

    val progress: StateFlow<VideoRenderProgress> = videoGenerator.progress

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun generateVideo(routeId: Long) {
        if (_isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true

            val points = trackPointRepository.getFilteredPointsForRouteSync(routeId)
            val photos = routePhotoRepository.getPhotosForRouteSync(routeId)

            val outputPath = videoGenerator.generateVideo(
                routeId = routeId,
                points = points,
                photos = photos,
                config = VideoConfig()
            )

            if (outputPath != null) {
                routeRepository.updateVideoPath(routeId, outputPath)
            }

            _isGenerating.value = false
        }
    }
}
