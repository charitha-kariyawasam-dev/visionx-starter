/*
 * Copyright (c) 2025 Charitha Kariyawasam
 */

package com.ckbits.ai.model.ProcessingTasks

import com.ckbits.ai.model.ProcessingTask
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class GrayscaleTask : ProcessingTask {
    override val name = "Grayscale"
    override val description = "Converts image to grayscale."
    override val parameters = emptyList<com.ckbits.ai.model.TaskParameter>()
    override fun process(input: Mat, params: Map<String, Any>): Mat {
        val output = Mat()
        Imgproc.cvtColor(input, output, Imgproc.COLOR_BGR2GRAY)
        val displayMat = Mat()
        Imgproc.cvtColor(output, displayMat, Imgproc.COLOR_GRAY2BGRA)
        output.release()
        return displayMat
    }
}
