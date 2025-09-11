/*
 * Copyright (c) 2025 Charitha Kariyawasam
 */

package com.ckbits.ai.model

import com.ckbits.ai.model.ProcessingTasks.EdgeDetectionTask
import com.ckbits.ai.model.ProcessingTasks.GrayscaleTask

/**
 * Registry for all available ProcessingTask implementations.
 */
object TaskRegistry {
    val tasks: List<ProcessingTask> = listOf(
        EdgeDetectionTask(),
        GrayscaleTask()
    )
}
