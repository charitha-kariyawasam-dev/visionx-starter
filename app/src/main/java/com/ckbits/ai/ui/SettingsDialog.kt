/*
 * Copyright (c) 2025 Charitha Kariyawasam
 */

package com.ckbits.ai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ckbits.ai.model.CameraResolution
import com.ckbits.ai.viewmodel.CameraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    overlayExpanded: MutableState<Boolean>,
    cameraViewModel: CameraViewModel,
    resDropdownExpanded: MutableState<Boolean>,
    taskDropdownExpanded: MutableState<Boolean>,
    supportedResolutions: List<CameraResolution>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.10f))
            .clickable(
                onClick = { overlayExpanded.value = false },
                indication = null,
                interactionSource = remember { MutableInteractionSource() })
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f)),
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(min = 320.dp, max = 480.dp)
                .fillMaxHeight(0.92f)
                .padding(4.dp)
                .zIndex(2f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader("Camera Source")
                CameraSourceSegmented(cameraViewModel)
                SectionHeader("Resolution")
                ResolutionDropdownDense(cameraViewModel, resDropdownExpanded, supportedResolutions)
                SectionHeader("Task")
                TaskDropdownDense(cameraViewModel, taskDropdownExpanded)
                SectionHeader("Task Parameters")
                TaskParameterControlsDense(cameraViewModel)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
private fun CameraSourceSegmented(cameraViewModel: CameraViewModel) {
    val options = listOf("Back", "Front")
    val selectedIndex =
        options.indexOfFirst { cameraViewModel.cameraSelector.value.name == it.uppercase() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            OutlinedButton(
                onClick = {
                    cameraViewModel.setCameraSelector(
                        if (label == "Back") com.ckbits.ai.model.CameraSelector.BACK else com.ckbits.ai.model.CameraSelector.FRONT
                    )
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent,
                    contentColor = Color.White
                ),
                border = if (selected) BorderStroke(2.dp, Color.White) else null,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f)
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolutionDropdownDense(
    cameraViewModel: CameraViewModel,
    resDropdownExpanded: MutableState<Boolean>,
    supportedResolutions: List<CameraResolution>
) {
    val currentRes = cameraViewModel.cameraResolution.value
    ExposedDropdownMenuBox(
        expanded = resDropdownExpanded.value,
        onExpandedChange = { resDropdownExpanded.value = it }
    ) {
        OutlinedTextField(
            value = "${currentRes.width} x ${currentRes.height}",
            onValueChange = {},
            readOnly = true,
            label = { Text("Select", color = Color.White) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = resDropdownExpanded.value) },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = resDropdownExpanded.value,
            onDismissRequest = { resDropdownExpanded.value = false },
            modifier = Modifier.background(Color.Black.copy(alpha = 0.85f))
        ) {
            supportedResolutions.forEach { res ->
                DropdownMenuItem(
                    text = { Text("${res.width} x ${res.height}", color = Color.White) },
                    onClick = {
                        cameraViewModel.setCameraResolution(res)
                        resDropdownExpanded.value = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDropdownDense(
    cameraViewModel: CameraViewModel,
    taskDropdownExpanded: MutableState<Boolean>
) {
    val tasks = cameraViewModel.availableTasks.map { it.name }
    val currentTask = cameraViewModel.processingTask.value
    ExposedDropdownMenuBox(
        expanded = taskDropdownExpanded.value,
        onExpandedChange = { taskDropdownExpanded.value = it }
    ) {
        OutlinedTextField(
            value = currentTask,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select", color = Color.White) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taskDropdownExpanded.value) },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = taskDropdownExpanded.value,
            onDismissRequest = { taskDropdownExpanded.value = false },
            modifier = Modifier.background(Color.Black.copy(alpha = 0.85f))
        ) {
            tasks.forEach { task ->
                DropdownMenuItem(
                    text = { Text(task, color = Color.White) },
                    onClick = {
                        cameraViewModel.setProcessingTask(task)
                        taskDropdownExpanded.value = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TaskParameterControlsDense(cameraViewModel: CameraViewModel) {
    val currentTask = cameraViewModel.processingTask.value
    val parameters = cameraViewModel.getProcessingTaskByName(currentTask)?.parameters ?: emptyList()
    val currentParams = cameraViewModel.taskParameters.value

    if (parameters.isEmpty()) {
        Text(
            "(No parameters for this task)",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    // Dynamically display all parameters for the current task
    parameters.forEach { param ->
        when (param.type) {
            com.ckbits.ai.model.ParameterType.FLOAT -> {
                val value = (currentParams[param.key] as? Float) ?: param.defaultValue as Float
                LabeledSliderDense(
                    label = param.label,
                    value = value,
                    valueRange = param.range as ClosedFloatingPointRange<Float>,
                    onValueChange = { cameraViewModel.setTaskParameter(param.key, it) },
                    valueDisplay = "%.0f".format(value)
                )
            }

            com.ckbits.ai.model.ParameterType.INT -> {
                val value = (currentParams[param.key] as? Int) ?: param.defaultValue as Int
                LabeledSliderDense(
                    label = param.label,
                    value = value.toFloat(),
                    valueRange = (param.range as IntRange).let { it.first.toFloat()..it.last.toFloat() },
                    onValueChange = { cameraViewModel.setTaskParameter(param.key, it.toInt()) },
                    valueDisplay = value.toString()
                )
            }

            com.ckbits.ai.model.ParameterType.BOOL,
            com.ckbits.ai.model.ParameterType.STRING,
            com.ckbits.ai.model.ParameterType.ENUM -> {
                // TODO: Implement UI controls for BOOL, STRING, and ENUM parameters when needed
                Text(
                    "Parameter type ${param.type} not yet implemented",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun LabeledSliderDense(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueDisplay: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            modifier = Modifier.width(80.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
        Text(
            valueDisplay,
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}
