/*
 * Copyright (c) 2025 Charitha Kariyawasam
 */

package com.ckbits.ai.model

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.Mat

class GenericAnalyzer(
    private val processingTask: ProcessingTask,
    private val parameters: Map<String, Any>,
    private val onBitmap: (Bitmap) -> Unit,
    private val onFpsUpdate: (Int) -> Unit,
    private val onResolutionUpdate: (CameraResolution) -> Unit
) : ImageAnalysis.Analyzer {

    private var frameCount = 0
    private var lastTimestamp = System.currentTimeMillis()
    private var hasReportedResolution = false

    override fun analyze(image: ImageProxy) {
        // Report actual image resolution once
        if (!hasReportedResolution) {
            onResolutionUpdate(CameraResolution(image.width, image.height))
            hasReportedResolution = true
        }

        // Calculate FPS
        frameCount++
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastTimestamp
        if (elapsed >= 1000) {
            val fps = (frameCount * 1000 / elapsed).toInt()
            onFpsUpdate(fps)
            frameCount = 0
            lastTimestamp = currentTime
        }

        // Process image
        val bitmap = imageProxyToBitmap(image, image.imageInfo.rotationDegrees)
        bitmap?.let {
            val mat = Mat()
            Utils.bitmapToMat(it, mat)
            val processedMat = processingTask.process(mat, parameters)
            val processedBitmap = Bitmap.createBitmap(
                processedMat.cols(),
                processedMat.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(processedMat, processedBitmap)
            onBitmap(processedBitmap)

            // Clean up
            mat.release()
            processedMat.release()
        }

        image.close()
    }

    private fun imageProxyToBitmap(image: ImageProxy, rotationDegrees: Int): Bitmap? {
        val width = image.width
        val height = image.height
        val yPlane = image.planes[0].buffer
        val uPlane = image.planes[1].buffer
        val vPlane = image.planes[2].buffer
        val yRowStride = image.planes[0].rowStride
        val uvRowStride = image.planes[1].rowStride
        val uvPixelStride = image.planes[1].pixelStride
        val argb = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val yIndex = y * yRowStride + x
                val uvIndex = (y shr 1) * uvRowStride + (x shr 1) * uvPixelStride
                val yValue = yPlane.get(yIndex).toInt() and 0xFF
                val uValue = uPlane.get(uvIndex).toInt() and 0xFF
                val vValue = vPlane.get(uvIndex).toInt() and 0xFF

                // Convert YUV to RGB
                var r = (yValue + 1.370705f * (vValue - 128)).toInt()
                var g = (yValue - 0.337633f * (uValue - 128) - 0.698001f * (vValue - 128)).toInt()
                var b = (yValue + 1.732446f * (uValue - 128)).toInt()
                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)
                argb[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val bitmap = Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888)

        // Apply rotation if needed
        return if (rotationDegrees != 0) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        } else {
            bitmap
        }
    }
}
