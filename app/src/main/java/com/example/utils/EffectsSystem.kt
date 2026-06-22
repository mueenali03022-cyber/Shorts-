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
    val isShake: Boolean = false
)

object EffectsSystem {
    val categories = listOf(
        "Trending", "AI effects", "Face effects", "AR effects", 
        "Background effects", "Glitch", "Shake", "Blur", "Light", "3D", "Fun effects"
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
                val effect = when (category) {
                    "Glitch" -> CameraEffect("e_$idCounter", "$category $i", category, isGlitch = true)
                    "Shake" -> CameraEffect("e_$idCounter", "$category $i", category, isShake = true)
                    "Blur" -> CameraEffect("e_$idCounter", "$category $i", category, blur = 5f + (i * 0.5f))
                    "3D" -> CameraEffect("e_$idCounter", "$category $i", category, scale = 1.1f + (i * 0.01f), rotation = i * 2f)
                    "Face effects" -> CameraEffect("e_$idCounter", "$category $i", category, scale = 1.05f) // Simulated
                    "AR effects" -> CameraEffect("e_$idCounter", "$category $i", category, scale = 0.9f)
                    "Trending" -> CameraEffect("e_$idCounter", "$category $i", category, scale = 1f + (i * 0.02f), rotation = i * 1f)
                    "Light" -> CameraEffect("e_$idCounter", "$category $i", category, alpha = 0.7f + (i % 10) * 0.03f)
                    else -> CameraEffect("e_$idCounter", "$category $i", category, scale = 1f + (i * 0.01f))
                }
                list.add(effect)
                idCounter++
            }
        }
        list
    }
}
