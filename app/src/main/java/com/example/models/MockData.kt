package com.example.models

object MockData {
    val currentUser = User(
        id = "u1",
        username = "mueenali",
        name = "Mueen Ali",
        profilePhoto = "https://i.pravatar.cc/150?u=mueen",
        bio = "Building awesome Android apps. Creator of Vizo.",
        followers = 1205,
        following = 45,
        likes = 15300
    )

    val videos = listOf(
        VideoItem(
            id = "v1",
            userId = "u2",
            username = "naturelover",
            userPhoto = "https://i.pravatar.cc/150?u=nature",
            videoUrl = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4",
            caption = "Check out this beautiful scenery! #nature #peace",
            music = "Relaxing Flute - Nature",
            likes = 1245,
            comments = 89,
            shares = 45
        ),
        VideoItem(
            id = "v2",
            userId = "u3",
            username = "videocreator",
            userPhoto = "https://i.pravatar.cc/150?u=creator",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            caption = "Wait for the end! 🔥 #fire #wow",
            music = "Trending Sound",
            likes = 8900,
            comments = 432,
            shares = 1200
        ),
        VideoItem(
            id = "v3",
            userId = currentUser.id,
            username = currentUser.username,
            userPhoto = currentUser.profilePhoto,
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
            caption = "My new setup! Let me know what you think. #tech #setup",
            music = "Original Sound - mueenali",
            likes = 456,
            comments = 23,
            shares = 12
        ),
        VideoItem(
            id = "v4",
            userId = "u4",
            username = "animemaster",
            userPhoto = "https://i.pravatar.cc/150?u=anime",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            caption = "Sci-fi shorts are the best! 🤖 #scifi #edit",
            music = "Cyberpunk Vibes",
            likes = 3400,
            comments = 156,
            shares = 890
        )
    )
}
