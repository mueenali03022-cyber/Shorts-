package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.io.File

class FaceLandmarkerHelper(
    private val context: Context,
    private val faceLandmarkerListener: LandmarkerListener? = null
) {
    private var faceLandmarker: FaceLandmarker? = null

    init {
        setupFaceLandmarker()
    }

    fun clearFaceLandmarker() {
        faceLandmarker?.close()
        faceLandmarker = null
    }

    private fun setupFaceLandmarker() {
        val baseOptionBuilder = BaseOptions.builder()
        baseOptionBuilder.setDelegate(Delegate.CPU)
        baseOptionBuilder.setModelAssetPath("face_landmarker.task")

        try {
            val baseOptions = baseOptionBuilder.build()
            val optionsBuilder =
                FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinFaceDetectionConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setMinFacePresenceConfidence(0.5f)
                    .setNumFaces(1)
                    .setOutputFaceBlendshapes(true)
                    .setOutputFacialTransformationMatrixes(true)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::returnLivestreamError)

            faceLandmarker = FaceLandmarker.createFromOptions(context, optionsBuilder.build())
        } catch (e: Exception) {
            faceLandmarkerListener?.onError("Face Landmarker failed to initialize. See error logs for details")
            Log.e("FaceLandmarkerHelper", "Face Landmarker failed to load model with error: " + e.message)
        }
    }

    private var lastFrameTime = 0L

    fun detectLiveStream(
        imageProxy: ImageProxy,
        isFrontCamera: Boolean
    ) {
        if (faceLandmarker == null) {
            imageProxy.close()
            return
        }
        var frameTime = SystemClock.uptimeMillis()
        if (frameTime <= lastFrameTime) {
            frameTime = lastFrameTime + 1
        }
        lastFrameTime = frameTime

        val bitmapBuffer = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees.toFloat()
        imageProxy.close()

        val matrix = Matrix().apply {
            postRotate(rotationDegrees)
        }
        var rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )
        if (rotatedBitmap != bitmapBuffer) {
            bitmapBuffer.recycle()
        }

        if (isFrontCamera) {
            val flipMatrix = Matrix()
            flipMatrix.postScale(-1f, 1f, rotatedBitmap.width / 2f, rotatedBitmap.height / 2f)
            val flippedBitmap = Bitmap.createBitmap(
                rotatedBitmap, 0, 0, rotatedBitmap.width, rotatedBitmap.height,
                flipMatrix, true
            )
            rotatedBitmap.recycle()
            rotatedBitmap = flippedBitmap
        }

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        detectAsync(mpImage, frameTime)
    }

    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        try {
            faceLandmarker?.detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            Log.e("FaceLandmarkerHelper", "Error during face detection", e)
        }
    }

    private fun returnLivestreamResult(
        result: FaceLandmarkerResult,
        input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        faceLandmarkerListener?.onResults(
            ResultBundle(
                result,
                input.width,
                input.height
            )
        )
    }

    private fun returnLivestreamError(error: RuntimeException) {
        faceLandmarkerListener?.onError(error.message ?: "An unknown error has occurred")
    }

    data class ResultBundle(
        val result: FaceLandmarkerResult,
        val inputImageWidth: Int,
        val inputImageHeight: Int,
    )

    interface LandmarkerListener {
        fun onError(error: String)
        fun onResults(resultBundle: ResultBundle)
    }
}
