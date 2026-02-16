package com.bioacoustic.visualizer.core.model

data class VisualPoint(
    val id: Long,
    val position: FloatArray, // [x, y, z]
    val color: FloatArray,    // [r, g, b, a]
    val size: Float,
    val intensity: Float,
    val timestamp: Long
)

