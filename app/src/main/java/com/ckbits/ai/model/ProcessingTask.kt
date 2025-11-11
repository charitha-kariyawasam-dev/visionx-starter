/*
 * Copyright (c) 2025 Charitha Kariyawasam
 */

package com.ckbits.ai.model

import org.opencv.core.Mat

/**
 * Generic interface for image processing or machine learning tasks/pipelines.
 * Each task exposes its name, description, configurable parameters, and a process method.
 */
interface ProcessingTask {
    val name: String
    val description: String
    val parameters: List<TaskParameter>

    /**
     * Processes the input frame using the provided parameters.
     * @param input The input frame (OpenCV Mat)
     * @param params Map of parameter key to value
     * @return The processed frame (OpenCV Mat)
     */
    fun process(input: Mat, params: Map<String, Any>): Mat
}
