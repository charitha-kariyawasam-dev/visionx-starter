/*
 * Copyright (c) 2025 Charitha Kariyawasam
 */

package com.ckbits.ai.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ckbits.ai.model.CameraResolution
import com.ckbits.ai.model.CameraSelector
import com.ckbits.ai.model.ProcessingTask
import com.ckbits.ai.model.ProcessingTasks.GrayscaleTask
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    // Camera state
    val hasPermission = mutableStateOf(false)
    val cameraProvider = mutableStateOf<ProcessCameraProvider?>(null)
    val preview = mutableStateOf<Preview?>(null)
    val imageAnalyzer = mutableStateOf<ImageAnalysis?>(null)
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Image processing state
    val processedBitmap = mutableStateOf<Bitmap?>(null)
    val analysisFps = mutableStateOf(0)
    val actualAnalysisResolution = mutableStateOf(CameraResolution(0, 0))

    // Camera configuration
    val supportedResolutions = mutableStateOf<List<CameraResolution>>(emptyList())
    val cameraSelector = mutableStateOf<CameraSelector>(CameraSelector.BACK)
    val cameraResolution = mutableStateOf(CameraResolution(1280, 720))

    // Processing task configuration
    val processingTask = mutableStateOf("Edge Detection")
    val taskParameters = mutableStateOf<Map<String, Any>>(emptyMap())
    val availableTasks = com.ckbits.ai.model.TaskRegistry.tasks

    // Public API
    fun setPermission(granted: Boolean) {
        hasPermission.value = granted
    }

    fun setAnalysisFps(value: Int) {
        analysisFps.value = value
    }

    fun setActualAnalysisResolution(resolution: CameraResolution) {
        actualAnalysisResolution.value = resolution
    }

    fun setProcessedBitmap(bitmap: Bitmap?) {
        processedBitmap.value = bitmap
    }

    fun setCameraResolution(resolution: CameraResolution) {
        cameraResolution.value = resolution
    }

    fun setCameraSelector(selector: CameraSelector) {
        cameraSelector.value = selector
    }

    fun setProcessingTask(task: String) {
        processingTask.value = task
        taskParameters.value = getDefaultParametersForTask(task)
    }

    fun setTaskParameter(key: String, value: Any) {
        val params = taskParameters.value.toMutableMap()
        params[key] = value
        taskParameters.value = params
    }

    fun getProcessingTaskByName(name: String): ProcessingTask? {
        return availableTasks.find { it.name == name }
    }

    fun setupCamera(context: Context, onBitmap: (Bitmap) -> Unit) {
        viewModelScope.launch {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()
                cameraProvider.value = provider

                val res = cameraResolution.value
                val resolutionSelector = createResolutionSelector(res)

                val previewUseCase = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                preview.value = previewUseCase

                val analysisUseCase = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val analyzer = createAnalyzerForTask(
                    getProcessingTaskByName(processingTask.value),
                    taskParameters.value,
                    onBitmap,
                    { setAnalysisFps(it) },
                    { setActualAnalysisResolution(it) }
                )
                analysisUseCase.setAnalyzer(cameraExecutor, analyzer)
                imageAnalyzer.value = analysisUseCase
            }, ContextCompat.getMainExecutor(context))
        }
    }

    fun querySupportedResolutions(context: Context) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIdList = cameraManager.cameraIdList
        val targetLensFacing = if (cameraSelector.value == CameraSelector.BACK) {
            CameraCharacteristics.LENS_FACING_BACK
        } else {
            CameraCharacteristics.LENS_FACING_FRONT
        }

        val resolutions = mutableListOf<CameraResolution>()
        for (id in cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (lensFacing == targetLensFacing) {
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val analysisSizes = map?.getOutputSizes(android.graphics.ImageFormat.YUV_420_888)
                analysisSizes?.forEach { size ->
                    resolutions.add(CameraResolution(size.width, size.height))
                }
                break
            }
        }

        val uniqueResolutions = resolutions
            .distinctBy { Pair(it.width, it.height) }
            .sortedBy { it.width * it.height }
        supportedResolutions.value = uniqueResolutions
        validateCameraResolution()
    }

    fun onCameraReady(context: Context) {
        querySupportedResolutions(context)
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
    }

    // Private helper methods
    private fun getDefaultParametersForTask(task: String): Map<String, Any> {
        val processingTask = getProcessingTaskByName(task)
        return processingTask?.parameters?.associate { param ->
            param.key to param.defaultValue
        } ?: emptyMap()
    }

    private fun createResolutionSelector(resolution: CameraResolution): ResolutionSelector {
        val selectedSize = android.util.Size(resolution.width, resolution.height)
        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    AspectRatio.RATIO_16_9,
                    AspectRatioStrategy.FALLBACK_RULE_AUTO
                )
            )
            .setResolutionStrategy(
                ResolutionStrategy(
                    selectedSize,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                )
            )
            .build()
    }

    private fun createAnalyzerForTask(
        task: ProcessingTask?,
        params: Map<String, Any>,
        onBitmap: (Bitmap) -> Unit,
        onFpsUpdate: (Int) -> Unit,
        onResolutionUpdate: (CameraResolution) -> Unit
    ): ImageAnalysis.Analyzer {
        return com.ckbits.ai.model.GenericAnalyzer(
            task ?: GrayscaleTask(),
            params,
            onBitmap,
            onFpsUpdate,
            onResolutionUpdate
        )
    }

    private fun validateCameraResolution() {
        val current = cameraResolution.value
        val supported = supportedResolutions.value
        if (supported.isNotEmpty() && supported.none { it.width == current.width && it.height == current.height }) {
            cameraResolution.value = supported.last()
        }
    }
}
