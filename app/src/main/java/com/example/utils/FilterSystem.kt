package com.example.utils

import androidx.compose.ui.graphics.ColorMatrix

data class CameraFilter(
    val id: String,
    val name: String,
    val category: String,
    val matrix: FloatArray
)

object FilterSystem {
    val categories = listOf("Beauty", "Color", "Vintage", "Cinematic", "B&W", "Warm", "Cool")
    
    val presets: List<CameraFilter> by lazy {
        val list = mutableListOf<CameraFilter>()
        var idCounter = 1
        
        list.add(
            CameraFilter(
                "f_0", "Normal", "Color", 
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        
        categories.forEach { category ->
            for (i in 1..80) {
                val filter = when (category) {
                    "B&W" -> {
                        val intensity = 0.5f + (i * 0.005f)
                        floatArrayOf(
                            0.33f * intensity, 0.59f * intensity, 0.11f * intensity, 0f, 0f,
                            0.33f * intensity, 0.59f * intensity, 0.11f * intensity, 0f, 0f,
                            0.33f * intensity, 0.59f * intensity, 0.11f * intensity, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    }
                    "Vintage" -> {
                        val rScale = 0.9f + (i * 0.001f)
                        val gScale = 0.7f + (i * 0.002f)
                        val bScale = 0.5f + (i * 0.003f)
                        floatArrayOf(
                            rScale, 0.2f, 0.1f, 0f, 0f,
                            0.1f, gScale, 0.1f, 0f, 0f,
                            0.1f, 0.1f, bScale, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    }
                    "Cinematic" -> {
                        val contrast = 1.1f + (i * 0.002f)
                        floatArrayOf(
                            contrast, 0f, 0f, 0f, -0.05f * 255,
                            0f, contrast, 0f, 0f, -0.02f * 255,
                            0f, 0f, contrast * 1.2f, 0f, 0.05f * 255,
                            0f, 0f, 0f, 1f, 0f
                        )
                    }
                    "Warm" -> {
                        val warmAmount = i * 0.005f
                        floatArrayOf(
                            1f + warmAmount, 0f, 0f, 0f, 0f,
                            0f, 1f + (warmAmount / 2), 0f, 0f, 0f,
                            0f, 0f, 1f - warmAmount, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    }
                    "Cool" -> {
                        val coolAmount = i * 0.005f
                        floatArrayOf(
                            1f - coolAmount, 0f, 0f, 0f, 0f,
                            0f, 1f, 0f, 0f, 0f,
                            0f, 0f, 1f + coolAmount, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    }
                    "Beauty" -> {
                        // Simulate skin smoothing / glow with slight brightness & red emphasis
                        floatArrayOf(
                            1.05f, 0f, 0f, 0f, 10f,
                            0f, 1.02f, 0f, 0f, 5f,
                            0f, 0f, 1.02f, 0f, 5f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    }
                    else -> {
                        val rScale = 0.8f + (Math.random().toFloat() * 0.4f)
                        val gScale = 0.8f + (Math.random().toFloat() * 0.4f)
                        val bScale = 0.8f + (Math.random().toFloat() * 0.4f)
                        floatArrayOf(
                            rScale, 0f, 0f, 0f, 0f,
                            0f, gScale, 0f, 0f, 0f,
                            0f, 0f, bScale, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    }
                }
                list.add(CameraFilter("f_${idCounter}", "$category $i", category, filter))
                idCounter++
            }
        }
        list
    }
    
    fun getIntensityMatrix(baseMatrix: FloatArray, intensity: Float): FloatArray {
        // Interpolate between Identity Matrix and Target Matrix based on intensity (0.0 to 1.0)
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        val result = FloatArray(20)
        for (i in 0..19) {
            result[i] = identity[i] + (baseMatrix[i] - identity[i]) * intensity
        }
        return result
    }
}
