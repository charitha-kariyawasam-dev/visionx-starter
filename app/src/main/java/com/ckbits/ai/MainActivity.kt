/*
 * Copyright (c) 2025 Charitha Kariyawasam
 */

package com.ckbits.ai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ckbits.ai.ui.CameraScreen
import com.ckbits.ai.ui.theme.VisionXStarterTheme
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val loaded = OpenCVLoader.initDebug()
        Log.d("OpenCV", "OpenCV loaded: $loaded")
        setContent {
            VisionXStarterTheme {
                CameraScreen()
            }
        }
    }
}