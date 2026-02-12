package com.routerecorder.core.domain.model

/**
 * Configuration for video rendering.
 */
data class VideoConfig(
    val width: Int = 1080,
    val height: Int = 1920,
    val fps: Int = 30,
    val bitRate: Int = 8_000_000,
    val iFrameInterval: Int = 2,
    val photoDurationMs: Long = 3_000,
    val photoTransitionMs: Long = 500,
    val lineColor: Int = 0xFF4CAF50.toInt(),
    val lineWidthPx: Float = 8f,
    val backgroundColor: Int = 0xFF1A1A2E.toInt()
)

/**
 * Progress state of video rendering.
 */
data class VideoRenderProgress(
    val currentFrame: Int = 0,
    val totalFrames: Int = 0,
    val phase: VideoRenderPhase = VideoRenderPhase.PREPARING,
    val outputPath: String? = null,
    val errorMessage: String? = null
) {
    val progressPercent: Float
        get() = if (totalFrames > 0) currentFrame.toFloat() / totalFrames else 0f
}

enum class VideoRenderPhase {
    PREPARING,
    RENDERING,
    ENCODING,
    MUXING,
    COMPLETED,
    ERROR
}
