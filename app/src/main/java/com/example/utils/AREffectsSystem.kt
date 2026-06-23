package com.example.utils

enum class AREffectType {
    NONE,
    FACE_MASK,
    FACE_STICKER_SCATTER,
    FACE_DECORATION, // glasses, hats
    ANIME_EYES,
    BIG_EYES,
    FACE_RESHAPE,
    BEAUTY_SMOOTH,
    MAKEUP,
    GLOW,
    NEON,
    GLITCH,
    VHS,
    CINEMATIC,
    VINTAGE
}

data class AREffectConfig(
    val id: String,
    val name: String,
    val category: String,
    val type: AREffectType = AREffectType.NONE,
    val assetUrl: String? = null, // URL or local path for PNG/JSON/3D asset
    val emojiAsset: String? = null, // Fallback/simple asset
    val trackingPoints: List<Int> = emptyList(), // e.g. MediaPipe landmark indices
    val colorMatrix: FloatArray? = null,
    val thumbnailUrl: String? = null,
    val intensity: Float = 1.0f
)

object AREffectsSystem {
    val categories = listOf(
        "Beauty", "Face", "AR Masks", "Stickers", "Visual Filters"
    )

    val presets: List<AREffectConfig> by lazy {
        val list = mutableListOf<AREffectConfig>()
        var idCounter = 1

        list.add(AREffectConfig("e_0", "None", "Beauty", AREffectType.NONE))

        categories.forEach { category ->
            when (category) {
                "Beauty" -> {
                    list.add(AREffectConfig("e_${idCounter++}", "Smooth Lite", category, AREffectType.BEAUTY_SMOOTH, intensity = 0.5f, thumbnailUrl = "https://images.unsplash.com/photo-1515377905703-c4788e51af15?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Smooth Pro", category, AREffectType.BEAUTY_SMOOTH, intensity = 1.0f, thumbnailUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Glow", category, AREffectType.GLOW, intensity = 0.8f, thumbnailUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Glossy Lips", category, AREffectType.MAKEUP, emojiAsset = "💋", trackingPoints = listOf(0, 11, 12, 13, 14, 15, 16, 17), thumbnailUrl = "https://images.unsplash.com/photo-1596704017254-9b121068fb31?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Nails", category, AREffectType.MAKEUP, emojiAsset = "💅", thumbnailUrl = "https://images.unsplash.com/photo-1522337660859-02fbefca4702?w=200&h=200&fit=crop"))
                }
                "Face" -> {
                    list.add(AREffectConfig("e_${idCounter++}", "Big Eyes", category, AREffectType.BIG_EYES, trackingPoints = listOf(159, 145, 386, 374), thumbnailUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Face Reshape", category, AREffectType.FACE_RESHAPE, trackingPoints = listOf(10, 152, 234, 454), thumbnailUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Funny Clown", category, AREffectType.FACE_MASK, emojiAsset = "🤡", thumbnailUrl = "https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Alien", category, AREffectType.FACE_MASK, emojiAsset = "👽", thumbnailUrl = "https://images.unsplash.com/photo-1618331835717-801e976710b2?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Robot", category, AREffectType.FACE_MASK, emojiAsset = "🤖", thumbnailUrl = "https://images.unsplash.com/photo-1535378917042-10a22c95931a?w=200&h=200&fit=crop"))
                }
                "AR Masks" -> {
                    list.add(AREffectConfig("e_${idCounter++}", "Fox", category, AREffectType.FACE_MASK, emojiAsset = "🦊", assetUrl = "assets/masks/fox.png", thumbnailUrl = "https://images.unsplash.com/photo-1516934024742-b461fba47600?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Lion", category, AREffectType.FACE_MASK, emojiAsset = "🦁", assetUrl = "assets/masks/lion.png", thumbnailUrl = "https://images.unsplash.com/photo-1546182990-dffeafbe841d?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Panda", category, AREffectType.FACE_MASK, emojiAsset = "🐼", assetUrl = "assets/masks/panda.png", thumbnailUrl = "https://images.unsplash.com/photo-1564349683136-77e08dba1ef7?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Cool Glasses", category, AREffectType.FACE_DECORATION, emojiAsset = "🕶️", trackingPoints = listOf(168), assetUrl = "assets/decorations/glasses.png", thumbnailUrl = "https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Crown", category, AREffectType.FACE_DECORATION, emojiAsset = "👑", trackingPoints = listOf(10), assetUrl = "assets/decorations/crown.png", thumbnailUrl = "https://images.unsplash.com/photo-1521127474489-d524412fd439?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Cap", category, AREffectType.FACE_DECORATION, emojiAsset = "🧢", trackingPoints = listOf(10), assetUrl = "assets/decorations/cap.png", thumbnailUrl = "https://images.unsplash.com/photo-1588850561407-ed78c282e89b?w=200&h=200&fit=crop"))
                }
                "Stickers" -> {
                    list.add(AREffectConfig("e_${idCounter++}", "Stars", category, AREffectType.FACE_STICKER_SCATTER, emojiAsset = "⭐", assetUrl = "assets/stickers/stars.json", thumbnailUrl = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Hearts", category, AREffectType.FACE_STICKER_SCATTER, emojiAsset = "💖", assetUrl = "assets/stickers/hearts.json", thumbnailUrl = "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Fire", category, AREffectType.FACE_STICKER_SCATTER, emojiAsset = "🔥", assetUrl = "assets/stickers/fire.json", thumbnailUrl = "https://images.unsplash.com/photo-1498550744921-75f79806b8a7?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Butterflies", category, AREffectType.FACE_STICKER_SCATTER, emojiAsset = "🦋", assetUrl = "assets/stickers/butterflies.json", thumbnailUrl = "https://images.unsplash.com/photo-1550853024-fae8cd4be47f?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Magic Sparkles", category, AREffectType.FACE_STICKER_SCATTER, emojiAsset = "✨", assetUrl = "assets/stickers/sparkles.json", thumbnailUrl = "https://images.unsplash.com/photo-1614850523459-c2f4c699c52e?w=200&h=200&fit=crop"))
                }
                "Visual Filters" -> {
                    list.add(AREffectConfig("e_${idCounter++}", "Neon Cyber", category, AREffectType.NEON, colorMatrix = floatArrayOf(
                        1.5f, 0f, 0f, 0f, 40f,
                        0f, 0.5f, 0f, 0f, 0f,
                        0f, 0f, 1.5f, 0f, 40f,
                        0f, 0f, 0f, 1f, 0f
                    ), assetUrl = "assets/luts/neon.cube", thumbnailUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Cinematic", category, AREffectType.CINEMATIC, colorMatrix = floatArrayOf(
                        0.9f, 0f, 0f, 0f, -10f,
                        0f, 1.0f, 0f, 0f, 0f,
                        0f, 0f, 1.2f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    ), assetUrl = "assets/luts/cinematic.cube", thumbnailUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Vintage 90s", category, AREffectType.VINTAGE, colorMatrix = floatArrayOf(
                        0.9f, 0f, 0f, 0f, 20f,
                        0f, 0.8f, 0f, 0f, 10f,
                        0f, 0f, 0.7f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    ), assetUrl = "assets/shaders/vintage.frag", thumbnailUrl = "https://images.unsplash.com/photo-1493225457124-a1a2a5ea3761?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "VHS Tape", category, AREffectType.VHS, colorMatrix = floatArrayOf(
                        1.2f, 0f, 0f, 0f, 10f,
                        0f, 0.9f, 0f, 0f, 0f,
                        0f, 0f, 1.1f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    ), assetUrl = "assets/shaders/vhs.frag", thumbnailUrl = "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=200&h=200&fit=crop"))
                    list.add(AREffectConfig("e_${idCounter++}", "Glitch Matrix", category, AREffectType.GLITCH, colorMatrix = floatArrayOf(
                        1.1f, 0f, 0f, 0f, 20f,
                        0f, 1.1f, 0f, 0f, 0f,
                        0f, 0f, 1.1f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    ), assetUrl = "assets/shaders/glitch.frag", thumbnailUrl = "https://images.unsplash.com/photo-1593640408182-31c70c8268f5?w=200&h=200&fit=crop"))
                }
            }
        }
        list
    }
}
