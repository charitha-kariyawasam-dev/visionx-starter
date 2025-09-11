/*
 * Copyright (c) 2025 Charitha Kariyawasam
 */

package com.ckbits.ai.model

/**
 * Data class representing a configurable parameter for a ProcessingTask.
 */
data class TaskParameter(
    val key: String,
    val label: String,
    val type: ParameterType,
    val defaultValue: Any,
    val range: ClosedRange<Float>? = null,
    val options: List<Any>? = null
)

/**
 * Enum representing the type of a parameter for UI rendering and validation.
 */
enum class ParameterType {
    INT, FLOAT, BOOL, STRING, ENUM
}
