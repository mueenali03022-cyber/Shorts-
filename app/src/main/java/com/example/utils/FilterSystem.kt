package com.example.utils

import androidx.compose.ui.graphics.ColorMatrix

data class CameraFilter(
    val id: String,
    val name: String,
    val category: String,
    val matrix: FloatArray,
    val thumbnailUrl: String
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
                ),
                "https://images.unsplash.com/photo-1516259762381-22954d7d3ad2?w=200&h=200&fit=crop"
            )
        )
        
        categories.forEach { category ->
            for (i in 1..10) {
                val thumbnailUrl = when (category) {
                    "Beauty" -> "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&h=200&fit=crop"
                    "Color" -> "https://images.unsplash.com/photo-1526045612212-70caf35c14df?w=200&h=200&fit=crop"
                    "Vintage" -> "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=200&h=200&fit=crop"
                    "Cinematic" -> "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=200&h=200&fit=crop"
                    "B&W" -> "https://images.unsplash.com/photo-1509305717900-84f40f78cce9?w=200&h=200&fit=crop"
                    "Warm" -> "https://images.unsplash.com/photo-1473496169904-658ba7c44d8a?w=200&h=200&fit=crop"
                    "Cool" -> "https://images.unsplash.com/photo-1517783999520-f068d7431a60?w=200&h=200&fit=crop"
                    else -> "https://images.unsplash.com/photo-1516259762381-22954d7d3ad2?w=200&h=200&fit=crop"
                }

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
                list.add(CameraFilter("f_${idCounter}", "$category $i", category, filter, thumbnailUrl))
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
