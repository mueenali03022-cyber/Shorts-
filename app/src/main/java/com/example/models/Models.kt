package com.example.models

data class User(
    var id: String = "",
    var username: String = "",
    var name: String = "",
    var profilePhoto: String = "",
    var bio: String = "",
    var followers: Int = 0,
    var following: Int = 0,
    var likes: Int = 0,
    var followersList: List<String> = listOf(),
    var followingList: List<String> = listOf(),
    var savedVideos: List<String> = listOf()
)

data class VideoItem(
    var id: String = "",
    var userId: String = "",
    var username: String = "",
    var userPhoto: String = "",
    var videoUrl: String = "",
    var caption: String = "",
    var music: String = "Original Sound",
    var likes: Int = 0,
    var likedBy: List<String> = listOf(),
    var comments: Int = 0,
    var shares: Int = 0,
    var timestamp: Long = System.currentTimeMillis(),
    var hashtags: List<String> = listOf(),
    var privacy: String = "Public",
    var allowComments: Boolean = true,
    var allowDownloads: Boolean = true
)

data class Comment(
    var id: String = "",
    var videoId: String = "",
    var userId: String = "",
    var username: String = "",
    var userPhoto: String = "",
    var text: String = "",
    var timestamp: Long = 0
)

sealed class UploadState {
    object Idle : UploadState()
    object Uploading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

data class Notification(
    var id: String = "",
    var toUserId: String = "",
    var fromUserId: String = "",
    var fromUsername: String = "",
    var fromUserPhoto: String = "",
    var type: String = "", // "like", "comment", "follow"
    var videoId: String? = null,
    var message: String = "",
    var isRead: Boolean = false,
    var timestamp: Long = System.currentTimeMillis()
)
