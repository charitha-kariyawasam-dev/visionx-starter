/*
 * Copyright (c) 2025 Charitha Kariyawasam
 */

package com.ckbits.ai.ui

import android.Manifest
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ckbits.ai.BuildConfig
import com.ckbits.ai.R
import com.ckbits.ai.model.CameraResolution
import com.ckbits.ai.model.toCameraSelector
import com.ckbits.ai.viewmodel.CameraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = context as androidx.lifecycle.LifecycleOwner
    val cameraViewModel: CameraViewModel = viewModel()
    val hasPermission by cameraViewModel.hasPermission
    val cameraProvider = cameraViewModel.cameraProvider.value
    val preview = cameraViewModel.preview.value
    val imageAnalyzer = cameraViewModel.imageAnalyzer.value
    val processedBitmap by cameraViewModel.processedBitmap
    val analysisFps by cameraViewModel.analysisFps
    val actualAnalysisResolution by cameraViewModel.actualAnalysisResolution
    val overlayExpanded = remember { mutableStateOf(false) }
    val aboutDialogExpanded = remember { mutableStateOf(false) }
    val menuExpanded = remember { mutableStateOf(false) }
    val supportedResolutions by cameraViewModel.supportedResolutions
    val resDropdownExpanded = remember { mutableStateOf(false) }
    val taskDropdownExpanded = remember { mutableStateOf(false) }

    // Permission launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        cameraViewModel.setPermission(isGranted)
        if (!isGranted) {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Setup camera when configuration changes
    LaunchedEffect(
        hasPermission,
        cameraViewModel.cameraResolution.value,
        cameraViewModel.cameraSelector.value,
        cameraViewModel.taskParameters.value,
        cameraViewModel.processingTask.value
    ) {
        if (hasPermission) {
            cameraViewModel.setupCamera(
                context,
                onBitmap = { cameraViewModel.setProcessedBitmap(it) }
            )
            cameraViewModel.onCameraReady(context)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Bind CameraX use cases to lifecycle
    LaunchedEffect(preview, imageAnalyzer, cameraProvider, hasPermission) {
        if (hasPermission && preview != null && imageAnalyzer != null && cameraProvider != null) {
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraViewModel.cameraSelector.value.toCameraSelector(),
                    preview,
                    imageAnalyzer
                )
            } catch (_: Exception) {
                // Handle binding errors silently
            }
        }
    }

    // Update preview resolution to match analysis resolution
    LaunchedEffect(actualAnalysisResolution) {
        if (actualAnalysisResolution.width > 0 && actualAnalysisResolution.height > 0) {
            cameraViewModel.setActualAnalysisResolution(actualAnalysisResolution)
        }
    }

    // Refresh supported resolutions when camera source changes
    LaunchedEffect(cameraViewModel.cameraSelector.value) {
        cameraViewModel.querySupportedResolutions(context)
    }

    // Main UI layout
    Box(modifier = Modifier.fillMaxSize()) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            LandscapeLayout(
                preview = preview,
                cameraProvider = cameraProvider,
                processedBitmap = processedBitmap,
                actualAnalysisResolution = actualAnalysisResolution,
                analysisFps = analysisFps
            )
        } else {
            PortraitLayout(
                preview = preview,
                cameraProvider = cameraProvider,
                processedBitmap = processedBitmap,
                actualAnalysisResolution = actualAnalysisResolution,
                analysisFps = analysisFps
            )
        }

        // Settings button with dropdown menu
        Box(modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp)) {
            IconButton(onClick = { menuExpanded.value = true }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
            }
            DropdownMenu(
                expanded = menuExpanded.value,
                onDismissRequest = { menuExpanded.value = false },
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.large // More rounded corners
                    )
                    .padding(vertical = 4.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "Settings",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        overlayExpanded.value = true
                        menuExpanded.value = false
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "About",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        aboutDialogExpanded.value = true
                        menuExpanded.value = false
                    }
                )
            }
        }
        // Settings overlay
        if (overlayExpanded.value) {
            SettingsDialog(
                overlayExpanded = overlayExpanded,
                cameraViewModel = cameraViewModel,
                resDropdownExpanded = resDropdownExpanded,
                taskDropdownExpanded = taskDropdownExpanded,
                supportedResolutions = supportedResolutions
            )
        }
        // About dialog
        if (aboutDialogExpanded.value) {
            AboutDialog(aboutDialogExpanded)
        }
    }
}

@Composable
private fun LandscapeLayout(
    preview: androidx.camera.core.Preview?,
    cameraProvider: androidx.camera.lifecycle.ProcessCameraProvider?,
    processedBitmap: android.graphics.Bitmap?,
    actualAnalysisResolution: CameraResolution,
    analysisFps: Int
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left: Camera preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (preview != null && cameraProvider != null) {
                androidx.compose.runtime.key(preview) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            preview.surfaceProvider = previewView.surfaceProvider
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Text("Camera preview not available", color = Color.White)
            }
        }

        // Right: Processed preview
        ProcessedImageView(
            processedBitmap = processedBitmap,
            actualAnalysisResolution = actualAnalysisResolution,
            analysisFps = analysisFps,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun PortraitLayout(
    preview: androidx.camera.core.Preview?,
    cameraProvider: androidx.camera.lifecycle.ProcessCameraProvider?,
    processedBitmap: android.graphics.Bitmap?,
    actualAnalysisResolution: CameraResolution,
    analysisFps: Int
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top: Camera preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (preview != null && cameraProvider != null) {
                androidx.compose.runtime.key(preview) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            preview.surfaceProvider = previewView.surfaceProvider
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Text("Camera preview not available", color = Color.White)
            }
        }

        // Bottom: Processed preview
        ProcessedImageView(
            processedBitmap = processedBitmap,
            actualAnalysisResolution = actualAnalysisResolution,
            analysisFps = analysisFps,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun ProcessedImageView(
    processedBitmap: android.graphics.Bitmap?,
    actualAnalysisResolution: CameraResolution,
    analysisFps: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        processedBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Processed Camera Frame",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Resolution overlay
            Text(
                text = "${actualAnalysisResolution.width} x ${actualAnalysisResolution.height}",
                color = Color.White,
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            // FPS overlay
            Text(
                text = "FPS: $analysisFps",
                color = Color.White,
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        } ?: run {
            Text("Processed view not available", color = Color.White)
        }
    }
}

@Composable
private fun SettingsButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            Icons.Filled.Settings,
            contentDescription = "Settings",
            tint = Color.White
        )
    }
}

@Composable
fun AboutDialog(aboutDialogExpanded: MutableState<Boolean>) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { aboutDialogExpanded.value = false },
        title = {
            Text(
                "About",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(id = R.string.app_name),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "Version: " + BuildConfig.VERSION_NAME,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "Copyright (c) 2025 Charitha Kariyawasam",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.TextButton(
                    onClick = { aboutDialogExpanded.value = false }
                ) {
                    Text("OK", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    )
}
