/*
 * Copyright (c) 2025 Charitha Kariyawasam
 */

package com.ckbits.ai.model.ProcessingTasks

import com.ckbits.ai.model.ParameterType
import com.ckbits.ai.model.ProcessingTask
import com.ckbits.ai.model.TaskParameter
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class EdgeDetectionTask : ProcessingTask {
    override val name = "Edge Detection"
    override val description = "Detects edges using Canny algorithm with dual thresholds."
    override val parameters = listOf(
        TaskParameter(
            key = "lowThreshold",
            label = "Low Threshold",
            type = ParameterType.FLOAT,
            defaultValue = 50f,
            range = 0f..255f
        ),
        TaskParameter(
            key = "highThreshold",
            label = "High Threshold",
            type = ParameterType.FLOAT,
            defaultValue = 150f,
            range = 0f..255f
        )
    )

    override fun process(input: Mat, params: Map<String, Any>): Mat {
        val lowThreshold = (params["lowThreshold"] as? Float) ?: 50f
        val highThreshold = (params["highThreshold"] as? Float) ?: 150f

        // Convert to grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(input, grayMat, Imgproc.COLOR_BGR2GRAY)

        // Apply Gaussian blur to reduce noise
        val blurredMat = Mat()
        Imgproc.GaussianBlur(grayMat, blurredMat, org.opencv.core.Size(5.0, 5.0), 0.0)

        // Apply Canny edge detection
        val edgesMat = Mat()
        Imgproc.Canny(blurredMat, edgesMat, lowThreshold.toDouble(), highThreshold.toDouble())

        // Convert back to BGRA for display
        val displayMat = Mat()
        Imgproc.cvtColor(edgesMat, displayMat, Imgproc.COLOR_GRAY2BGRA)

        // Clean up intermediate matrices
        grayMat.release()
        blurredMat.release()
        edgesMat.release()

        return displayMat
    }
}
