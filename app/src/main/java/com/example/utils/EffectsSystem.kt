package com.example.utils

data class CameraEffect(
    val id: String,
    val name: String,
    val category: String,
    // Effect parameters
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val alpha: Float = 1f,
    val blur: Float = 0f,
    val isGlitch: Boolean = false,
    val isShake: Boolean = false,
    val colorMatrix: FloatArray? = null,
    val thumbnailUrl: String = "https://images.unsplash.com/photo-1516259762381-22954d7d3ad2?w=200&h=200&fit=crop"
)

object EffectsSystem {
    val categories = listOf(
        "Trending", "Face filters", "Beauty filter", "Skin smooth", "Eye enhancement",
        "Face reshape", "Color grading", "Vintage", "Cinematic", "Glitch", "Neon", "Blur", "Background effects"
    )
    
    val presets: List<CameraEffect> by lazy {
        val list = mutableListOf<CameraEffect>()
        var idCounter = 1
        
        list.add(
            CameraEffect("e_0", "None", "Trending")
        )
        
        categories.forEach { category ->
            // Create ~45 effects per category to get ~500 total
            for (i in 1..46) {
                val thumbnailUrl = when (category) {
                    "Trending" -> "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=200&h=200&fit=crop"
                    "Face filters", "Beauty filter", "Skin smooth", "Eye enhancement", "Face reshape" -> "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&h=200&fit=crop"
                    "Color grading", "Vintage", "Cinematic" -> "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=200&h=200&fit=crop"
                    "Glitch", "Neon" -> "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=200&h=200&fit=crop"
                    "Blur" -> "https://images.unsplash.com/photo-1556761175-5973e2182068?w=200&h=200&fit=crop"
                    "Background effects" -> "https://images.unsplash.com/photo-1506744626753-140285b73fe3?w=200&h=200&fit=crop"
                    else -> "https://images.unsplash.com/photo-1516259762381-22954d7d3ad2?w=200&h=200&fit=crop"
                }

                val effect = when (category) {
                    "Glitch" -> CameraEffect("e_$idCounter", "$category $i", category, isGlitch = true, thumbnailUrl = thumbnailUrl)
                    "Shake" -> CameraEffect("e_$idCounter", "$category $i", category, isShake = true, thumbnailUrl = thumbnailUrl)
                    "Blur" -> CameraEffect("e_$idCounter", "$category $i", category, blur = 3f + (i * 0.2f), thumbnailUrl = thumbnailUrl)
                    "Vintage", "Cinematic", "Color grading" -> {
                        val c = 0.8f + (i * 0.01f)
                        val m = floatArrayOf(
                            c, 0f, 0f, 0f, 0f,
                            0f, c*1.1f, 0f, 0f, 0f,
                            0f, 0f, c*0.9f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                        CameraEffect("e_$idCounter", "$category $i", category, colorMatrix = m, thumbnailUrl = thumbnailUrl)
                    }
                    "Neon" -> {
                        val m = floatArrayOf(
                            1.5f, 0f, 0f, 0f, 20f,
                            0f, 0.5f, 0f, 0f, 0f,
                            0f, 0f, 1.5f, 0f, 20f,
                            0f, 0f, 0f, 1f, 0f
                        )
                        CameraEffect("e_$idCounter", "$category $i", category, colorMatrix = m, thumbnailUrl = thumbnailUrl)
                    }
                    "Face filters", "Beauty filter", "Skin smooth", "Eye enhancement", "Face reshape" -> {
                        CameraEffect("e_$idCounter", "$category $i", category, blur = 0.5f + (i * 0.05f), scale = 1.05f, thumbnailUrl = thumbnailUrl)
                    }
                    "Background effects" -> CameraEffect("e_$idCounter", "$category $i", category, alpha = 0.8f, blur = 10f, thumbnailUrl = thumbnailUrl)
                    "Trending" -> CameraEffect("e_$idCounter", "$category $i", category, scale = 1f + (i * 0.02f), rotation = i * 1f, thumbnailUrl = thumbnailUrl)
                    else -> CameraEffect("e_$idCounter", "$category $i", category, scale = 1f + (i * 0.01f), thumbnailUrl = thumbnailUrl)
                }
                list.add(effect)
                idCounter++
            }
        }
        list
    }
}
