/*
 * Copyright (c) 2025 Charitha Kariyawasam
 */

package com.ckbits.ai.model

import androidx.camera.core.CameraSelector as CameraXSelector

data class CameraResolution(val width: Int, val height: Int)

enum class CameraSelector {
    FRONT,
    BACK
}

fun CameraSelector.toCameraSelector(): CameraXSelector {
    return when (this) {
        CameraSelector.BACK -> CameraXSelector.DEFAULT_BACK_CAMERA
        CameraSelector.FRONT -> CameraXSelector.DEFAULT_FRONT_CAMERA
    }
}
