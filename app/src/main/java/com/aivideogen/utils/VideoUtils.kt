package com.aivideogen.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.ThumbnailUtils
import android.os.Build
import android.util.Size
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer

object VideoUtils {

    /**
     * Extract a thumbnail from a video file.
     * Returns null if the video can't be read.
     */
    fun extractThumbnail(context: Context, videoPath: String): String? {
        return try {
            val thumbDir = FileUtils.getImagesDir(context)
            val thumbFile = File(thumbDir, "thumb_${System.currentTimeMillis()}.jpg")

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ThumbnailUtils.createVideoThumbnail(File(videoPath), Size(512, 288), null)
            } else {
                @Suppress("DEPRECATION")
                ThumbnailUtils.createVideoThumbnail(videoPath, android.provider.MediaStore.Images.Thumbnails.MINI_KIND)
            }

            bitmap?.let {
                thumbFile.outputStream().use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                thumbFile.absolutePath
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not extract thumbnail from $videoPath")
            null
        }
    }

    /**
     * Create a slideshow video from a list of image files.
     * Each image is displayed for [durationPerFrame] seconds.
     * Uses MediaCodec + MediaMuxer for hardware-accelerated encoding.
     */
    fun createSlideshow(
        context: Context,
        imageFiles: List<File>,
        totalDurationSec: Int = 10,
        fps: Int = 24
    ): String {
        val outputFile = File(FileUtils.getVideoOutputDir(context),
            "slideshow_${System.currentTimeMillis()}.mp4")

        val width  = 1280
        val height = 720
        val bitRate = 4_000_000  // 4 Mbps

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        val inputSurface = codec.createInputSurface()
        codec.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        val totalFrames = totalDurationSec * fps
        val framesPerImage = if (imageFiles.isNotEmpty()) totalFrames / imageFiles.size else totalFrames
        val usPerFrame = 1_000_000L / fps

        try {
            val canvas = inputSurface.lockCanvas(null)
            canvas?.let {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val bg = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bg.eraseColor(android.graphics.Color.BLACK)

                for (frameIndex in 0 until totalFrames) {
                    val imgIdx = if (imageFiles.isNotEmpty())
                        (frameIndex / framesPerImage).coerceAtMost(imageFiles.size - 1)
                    else 0

                    val bmp = if (imageFiles.isNotEmpty()) {
                        BitmapFactory.decodeFile(imageFiles[imgIdx].absolutePath)?.let { raw ->
                            scaleFit(raw, width, height)
                        } ?: bg
                    } else bg

                    val canvas2 = inputSurface.lockCanvas(null)
                    if (canvas2 != null) {
                        canvas2.drawBitmap(bmp, 0f, 0f, paint)
                        inputSurface.unlockCanvasAndPost(canvas2)
                    }

                    // Drain encoder
                    drainEncoder(codec, muxer, bufferInfo, frameIndex * usPerFrame,
                        false) { idx ->
                        trackIndex = idx
                        muxerStarted = true
                    }
                }
                inputSurface.unlockCanvasAndPost(canvas)
            }
        } finally {
            drainEncoder(codec, muxer, bufferInfo, (totalFrames) * usPerFrame,
                true) { idx -> if (trackIndex < 0) trackIndex = idx }
            codec.stop()
            codec.release()
            inputSurface.release()
            if (muxerStarted) muxer.stop()
            muxer.release()
        }

        return outputFile.absolutePath
    }

    /**
     * Encode a list of image frames into an MP4.
     */
    fun framesToVideo(context: Context, frames: List<File>, fps: Int = 24): String {
        return createSlideshow(context, frames, frames.size / fps, fps)
    }

    // ── Private helpers ───────────────────────

    private fun scaleFit(src: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val ratio = minOf(targetW.toFloat() / src.width, targetH.toFloat() / src.height)
        val w = (src.width  * ratio).toInt()
        val h = (src.height * ratio).toInt()
        val scaled = Bitmap.createScaledBitmap(src, w, h, true)
        val result = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(android.graphics.Color.BLACK)
        canvas.drawBitmap(scaled, ((targetW - w) / 2f), ((targetH - h) / 2f), null)
        if (scaled != src) scaled.recycle()
        return result
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        presentationTimeUs: Long,
        endOfStream: Boolean,
        onTrackAdded: (Int) -> Unit
    ) {
        if (endOfStream) encoder.signalEndOfInputStream()

        while (true) {
            val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
            when {
                encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                }
                encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val idx = muxer.addTrack(encoder.outputFormat)
                    onTrackAdded(idx)
                    muxer.start()
                }
                encoderStatus >= 0 -> {
                    val encodedData: ByteBuffer = encoder.getOutputBuffer(encoderStatus) ?: continue
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0) {
                        bufferInfo.presentationTimeUs = presentationTimeUs
                        muxer.writeSampleData(encoderStatus, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(encoderStatus, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
        }
    }
}
