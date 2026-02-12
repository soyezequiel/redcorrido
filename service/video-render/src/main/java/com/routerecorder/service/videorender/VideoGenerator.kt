package com.routerecorder.service.videorender

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.view.Surface
import com.routerecorder.core.domain.model.RoutePhoto
import com.routerecorder.core.domain.model.TrackPoint
import com.routerecorder.core.domain.model.VideoConfig
import com.routerecorder.core.domain.model.VideoRenderPhase
import com.routerecorder.core.domain.model.VideoRenderProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Video generation engine using MediaCodec + Canvas → Surface → H.264 → MP4.
 *
 * Pipeline:
 * 1. Creates H.264 encoder with Surface input
 * 2. For each frame, draws onto Canvas → Surface
 * 3. Encoder hardware-accelerates the encoding
 * 4. MediaMuxer writes to MP4 file
 *
 * This is the recommended approach per the architecture plan because:
 * - Zero-copy from Canvas to encoder Surface (no Bitmap→ByteBuffer conversion)
 * - Uses hardware encoder (GPU) on most devices
 * - Easily migratable to OpenGL ES for future 3D rendering
 */
@Singleton
class VideoGenerator @Inject constructor() {

    private val _progress = MutableStateFlow(VideoRenderProgress())
    val progress: StateFlow<VideoRenderProgress> = _progress.asStateFlow()

    /**
     * Generate a video for the given route data.
     * This is a long-running suspend function — call from a coroutine on Dispatchers.Default.
     *
     * @param routeId used for naming the output file
     * @param points all track points (filtered, ordered by timestamp)
     * @param photos associated photos with file paths
     * @param config video rendering configuration
     * @return the absolute path to the generated MP4 file, or null on failure
     */
    suspend fun generateVideo(
        routeId: Long,
        points: List<TrackPoint>,
        photos: List<RoutePhoto>,
        config: VideoConfig = VideoConfig()
    ): String? = withContext(Dispatchers.Default) {

        if (points.size < 2) {
            _progress.value = VideoRenderProgress(
                phase = VideoRenderPhase.ERROR,
                errorMessage = "Se necesitan al menos 2 puntos para generar un video"
            )
            return@withContext null
        }

        val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        outputDir.mkdirs()
        val outputFile = File(outputDir, "RouteRecorder_${routeId}_${System.currentTimeMillis()}.mp4")

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: Surface? = null

        try {
            _progress.value = VideoRenderProgress(phase = VideoRenderPhase.PREPARING)

            // Calculate video parameters
            val totalDurationMs = points.last().timestampMs - points.first().timestampMs
            val videoDurationSec = calculateVideoDuration(totalDurationMs)
            val totalFrames = videoDurationSec * config.fps
            val nsPerFrame = 1_000_000_000L / config.fps

            // Setup encoder
            val format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                config.width,
                config.height
            ).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, config.bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, config.fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, config.iFrameInterval)
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
            }

            // Find a suitable encoder
            val codecName = findEncoderForFormat(format)
            encoder = if (codecName != null) {
                MediaCodec.createByCodecName(codecName)
            } else {
                MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            }

            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            // Setup muxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false

            // Setup frame renderer
            val frameRenderer = FrameRenderer(
                config = config,
                allPoints = points,
                photos = photos,
                startTimeMs = points.first().timestampMs,
                totalDurationMs = totalDurationMs
            )

            // Pre-load photo bitmaps (resized to save memory)
            val photoBitmaps = preloadPhotos(photos, config.width, config.height)

            _progress.value = VideoRenderProgress(phase = VideoRenderPhase.RENDERING, totalFrames = totalFrames)

            // Render frames
            val bufferInfo = MediaCodec.BufferInfo()
            for (frameIndex in 0 until totalFrames) {
                val progress = frameIndex.toFloat() / (totalFrames - 1)

                // Check if a photo should be shown at this progress
                val (photoBitmap, photoAlpha) = getPhotoForProgress(
                    progress, photos, points, photoBitmaps, config, totalFrames, frameIndex
                )

                // Draw frame to Surface via Canvas
                val canvas = inputSurface.lockHardwareCanvas()
                try {
                    frameRenderer.renderFrame(canvas, progress, photoBitmap, photoAlpha)
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                // Set presentation time
                val presentationTimeNs = frameIndex.toLong() * nsPerFrame
                // Note: presentation time is set in the encoded output below

                // Drain encoder
                drainEncoder(encoder, muxer, bufferInfo, trackIndex, muxerStarted).let { (ti, ms) ->
                    trackIndex = ti
                    muxerStarted = ms
                }

                // Update progress
                _progress.value = VideoRenderProgress(
                    currentFrame = frameIndex + 1,
                    totalFrames = totalFrames,
                    phase = VideoRenderPhase.ENCODING
                )
            }

            // Signal end of stream
            encoder.signalEndOfInputStream()

            // Drain remaining
            _progress.value = _progress.value.copy(phase = VideoRenderPhase.MUXING)
            drainEncoder(encoder, muxer, bufferInfo, trackIndex, muxerStarted, endOfStream = true)

            // Cleanup photo bitmaps
            photoBitmaps.values.forEach { it.recycle() }

            _progress.value = VideoRenderProgress(
                currentFrame = totalFrames,
                totalFrames = totalFrames,
                phase = VideoRenderPhase.COMPLETED,
                outputPath = outputFile.absolutePath
            )

            outputFile.absolutePath

        } catch (e: Exception) {
            _progress.value = VideoRenderProgress(
                phase = VideoRenderPhase.ERROR,
                errorMessage = "Error generando video: ${e.message}"
            )
            outputFile.delete()
            null
        } finally {
            try { encoder?.stop() } catch (_: Exception) {}
            try { encoder?.release() } catch (_: Exception) {}
            try { inputSurface?.release() } catch (_: Exception) {}
            try { muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    /**
     * Calculate video duration in seconds.
     * Short routes (<5 min) → 15 sec video
     * Medium routes (5-60 min) → 30 sec video
     * Long routes (>60 min) → 45 sec video
     */
    private fun calculateVideoDuration(routeDurationMs: Long): Int {
        val routeMinutes = routeDurationMs / 60_000
        return when {
            routeMinutes < 5 -> 15
            routeMinutes < 60 -> 30
            else -> 45
        }
    }

    private fun findEncoderForFormat(format: MediaFormat): String? {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        return codecList.findEncoderForFormat(format)
    }

    private fun preloadPhotos(
        photos: List<RoutePhoto>,
        maxWidth: Int,
        maxHeight: Int
    ): Map<Long, Bitmap> {
        val result = mutableMapOf<Long, Bitmap>()
        for (photo in photos) {
            try {
                val file = File(photo.filePath)
                if (!file.exists()) continue

                // Decode with sample size for memory efficiency
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(photo.filePath, options)

                val sampleSize = calculateInSampleSize(
                    options.outWidth, options.outHeight,
                    (maxWidth * 0.6).toInt(), (maxHeight * 0.4).toInt()
                )

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565 // Save memory
                }
                val bitmap = BitmapFactory.decodeFile(photo.filePath, decodeOptions)
                if (bitmap != null) {
                    result[photo.id] = bitmap
                }
            } catch (_: Exception) {
                // Skip photos that can't be decoded
            }
        }
        return result
    }

    private fun calculateInSampleSize(
        rawWidth: Int, rawHeight: Int,
        reqWidth: Int, reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (rawHeight > reqHeight || rawWidth > reqWidth) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Check if a photo should be displayed at the current progress.
     * Returns the bitmap and alpha for fade transition.
     */
    private fun getPhotoForProgress(
        progress: Float,
        photos: List<RoutePhoto>,
        points: List<TrackPoint>,
        bitmaps: Map<Long, Bitmap>,
        config: VideoConfig,
        totalFrames: Int,
        currentFrame: Int
    ): Pair<Bitmap?, Float> {
        if (photos.isEmpty()) return Pair(null, 0f)

        val currentPointIndex = ((points.size - 1) * progress).toInt()
        if (currentPointIndex >= points.size) return Pair(null, 0f)
        val currentPoint = points[currentPointIndex]

        for (photo in photos) {
            val nearestPointIndex = points.indexOfFirst { it.id == photo.nearestPointId }
            if (nearestPointIndex < 0) continue

            // Photo appears for configurable duration centered on the nearest point
            val photoDurationFrames = (config.photoDurationMs * config.fps / 1000).toInt()
            val transitionFrames = (config.photoTransitionMs * config.fps / 1000).toInt()

            val photoProgressCenter = nearestPointIndex.toFloat() / (points.size - 1)
            val photoCenterFrame = (photoProgressCenter * totalFrames).toInt()
            val photoStartFrame = photoCenterFrame - photoDurationFrames / 2
            val photoEndFrame = photoCenterFrame + photoDurationFrames / 2

            if (currentFrame in photoStartFrame..photoEndFrame) {
                val bitmap = bitmaps[photo.id] ?: continue

                // Calculate alpha for fade in/out
                val alpha = when {
                    currentFrame < photoStartFrame + transitionFrames -> {
                        (currentFrame - photoStartFrame).toFloat() / transitionFrames
                    }
                    currentFrame > photoEndFrame - transitionFrames -> {
                        (photoEndFrame - currentFrame).toFloat() / transitionFrames
                    }
                    else -> 1.0f
                }

                return Pair(bitmap, alpha.coerceIn(0f, 1f))
            }
        }

        return Pair(null, 0f)
    }

    /**
     * Drain encoder output buffers to the muxer.
     */
    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        trackIndex: Int,
        muxerStarted: Boolean,
        endOfStream: Boolean = false
    ): Pair<Int, Boolean> {
        var ti = trackIndex
        var ms = muxerStarted
        val timeoutUs = if (endOfStream) 10_000L else 0L

        while (true) {
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)

            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFormat = encoder.outputFormat
                    ti = muxer.addTrack(newFormat)
                    muxer.start()
                    ms = true
                }
                outputIndex >= 0 -> {
                    val outputBuffer = encoder.getOutputBuffer(outputIndex) ?: continue

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size > 0 && ms) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(ti, outputBuffer, bufferInfo)
                    }

                    encoder.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
            }
        }

        return Pair(ti, ms)
    }
}
