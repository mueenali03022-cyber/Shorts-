package com.example.utils

enum class AREffectType {
    NONE,
    FACE_MASK,
    FACE_STICKER_SCATTER,
    FACE_DECORATION, // glasses, hats
    ANIME_EYES,
    BEAUTY_SMOOTH,
    MAKEUP,
    NEON,
    GLITCH,
    VHS,
    CINEMATIC,
    VINTAGE
}

data class AREffect(
    val id: String,
    val name: String,
    val category: String,
    val type: AREffectType = AREffectType.NONE,
    val emojiAsset: String? = null,
    val colorMatrix: FloatArray? = null
)

object AREffectsSystem {
    val categories = listOf(
        "Beauty", "Face", "Funny", "AR Masks", "Stickers",
        "Anime", "Gaming", "Neon", "Vintage", "Cinematic"
    )

    val presets: List<AREffect> by lazy {
        val list = mutableListOf<AREffect>()
        var idCounter = 1

        list.add(AREffect("e_0", "None", "Beauty", AREffectType.NONE))

        categories.forEach { category ->
            for (i in 1..6) { // Less items to keep it realistic and performant
                val effect = when (category) {
                    "Beauty" -> AREffect("e_$idCounter", "Smooth $i", category, AREffectType.BEAUTY_SMOOTH)
                    "Face" -> AREffect("e_$idCounter", "Makeup $i", category, AREffectType.MAKEUP, emojiAsset = listOf("💋", "💅", "💄")[i % 3])
                    "Funny" -> {
                        val emojis = listOf("🤡", "🥸", "👽", "🤪", "🤠", "🤖")
                        AREffect("e_$idCounter", "Funny $i", category, AREffectType.FACE_MASK, emojiAsset = emojis[(i - 1) % emojis.size])
                    }
                    "AR Masks" -> {
                        val masks = listOf("🦊", "🦁", "🐼", "🐯", "🐶", "🐰")
                        AREffect("e_$idCounter", "Mask $i", category, AREffectType.FACE_MASK, emojiAsset = masks[(i - 1) % masks.size])
                    }
                    "Stickers" -> {
                        val stickers = listOf("⭐", "💖", "✨", "🔥", "🌸", "🦋")
                        AREffect("e_$idCounter", "Sticker $i", category, AREffectType.FACE_STICKER_SCATTER, emojiAsset = stickers[(i - 1) % stickers.size])
                    }
                    "Anime" -> AREffect("e_$idCounter", "Anime Eyes $i", category, AREffectType.ANIME_EYES)
                    "Gaming" -> AREffect("e_$idCounter", "Gamer $i", category, AREffectType.FACE_DECORATION, emojiAsset = listOf("🕶️", "👑", "🧢")[i % 3])
                    "Neon" -> {
                        val m = floatArrayOf(
                            1.5f, 0f, 0f, 0f, 40f,
                            0f, 0.5f, 0f, 0f, 0f,
                            0f, 0f, 1.5f, 0f, 40f,
                            0f, 0f, 0f, 1f, 0f
                        )
                        AREffect("e_$idCounter", "Neon $i", category, AREffectType.NEON, colorMatrix = m)
                    }
                    "Vintage" -> {
                        val c = 0.8f + (i * 0.02f)
                        val m = floatArrayOf(
                            c, 0f, 0f, 0f, 20f,
                            0f, c*0.9f, 0f, 0f, 10f,
                            0f, 0f, c*0.7f, 0f, -10f,
                            0f, 0f, 0f, 1f, 0f
                        )
                        AREffect("e_$idCounter", "Vintage $i", category, AREffectType.VINTAGE, colorMatrix = m)
                    }
                    "Cinematic" -> {
                        val m = floatArrayOf(
                            0.9f, 0f, 0f, 0f, -10f,
                            0f, 1.0f, 0f, 0f, 0f,
                            0f, 0f, 1.2f, 0f, 20f,
                            0f, 0f, 0f, 1f, 0f
                        )
                        AREffect("e_$idCounter", "Cinema $i", category, AREffectType.CINEMATIC, colorMatrix = m)
                    }
                    else -> AREffect("e_$idCounter", "Effect $i", category, AREffectType.NONE)
                }
                list.add(effect)
                idCounter++
            }
        }
        list
    }
}
