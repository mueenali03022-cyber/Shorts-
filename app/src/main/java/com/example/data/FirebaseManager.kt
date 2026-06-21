package com.example.data

import android.content.Context
import android.net.Uri
import com.example.models.Comment
import com.example.models.UploadState
import com.example.models.User
import com.example.models.VideoItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class FirebaseManager {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    private val CLOUDINARY_CLOUD_NAME = "djye4q3vg"
    private val CLOUDINARY_UPLOAD_PRESET = "unsigned_preset"

    suspend fun uploadVideo(
        context: Context,
        user: User,
        uri: Uri,
        caption: String,
        privacy: String = "Public",
        allowComments: Boolean = true,
        allowDownloads: Boolean = true,
        onStateChange: (UploadState) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            onStateChange(UploadState.Uploading)
            val downloadUrl = uploadFileToCloudinary(context, uri)

            val hashtagRegex = "#\\w+".toRegex()
            val extractedHashtags = hashtagRegex.findAll(caption).map { it.value.lowercase() }.toList()

            val videoData = VideoItem(
                userId = user.id,
                username = user.username,
                userPhoto = user.profilePhoto,
                videoUrl = downloadUrl,
                caption = caption,
                hashtags = extractedHashtags,
                privacy = privacy,
                allowComments = allowComments,
                allowDownloads = allowDownloads,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("videos").add(videoData).await()
            onStateChange(UploadState.Success)
        } catch (e: Exception) {
            e.printStackTrace()
            onStateChange(UploadState.Error(e.localizedMessage ?: "Unknown error occurred"))
        }
    }

    private fun uploadFileToCloudinary(context: Context, uri: Uri): String {
        val client = OkHttpClient()
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open file")
        val bytes = inputStream.readBytes()
        inputStream.close()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
            .addFormDataPart(
                "file",
                "video.mp4",
                bytes.toRequestBody("video/mp4".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/video/upload")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw Exception("Failed to upload: $responseString")
        }

        val json = JSONObject(responseString)
        return json.getString("secure_url")
    }

    private fun uploadImageToCloudinary(context: Context, uri: Uri): String {
        val client = OkHttpClient()
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open file")
        val bytes = inputStream.readBytes()
        inputStream.close()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
            .addFormDataPart(
                "file",
                "image.jpg",
                bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw Exception("Failed to upload: $responseString")
        }

        val json = JSONObject(responseString)
        return json.getString("secure_url")
    }

    suspend fun updateUserProfile(
        userId: String,
        username: String,
        bio: String,
        photoUri: Uri?,
        context: Context
    ) = withContext(Dispatchers.IO) {
        try {
            var photoUrl: String? = null
            if (photoUri != null) {
                photoUrl = uploadImageToCloudinary(context, photoUri)
            }

            val updates = mutableMapOf<String, Any>()
            if (username.isNotBlank()) updates["username"] = username
            updates["bio"] = bio
            if (photoUrl != null) updates["profilePhoto"] = photoUrl

            firestore.collection("users").document(userId).update(updates).await()

            // Also update all videos by this user? Not requested explicitly, but good for consistency.
            // Let's keep it simple and just update the user doc.
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        var retryCount = 0
        var videos: List<VideoItem>? = null
        
        while (retryCount < 3) {
            try {
                val snapshot = firestore.collection("videos")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                videos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(VideoItem::class.java)?.copy(id = doc.id)
                }
                break
            } catch (e: Exception) {
                e.printStackTrace()
                retryCount++
                kotlinx.coroutines.delay(500)
            }
        }
        
        if (videos == null) {
            // Unlikely, but if internet is totally down, try cache explicitly
            try {
                val cacheSnapshot = firestore.collection("videos")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get(com.google.firebase.firestore.Source.CACHE)
                    .await()
                videos = cacheSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(VideoItem::class.java)?.copy(id = doc.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception("Failed to fetch videos from server and cache.")
            }
        }
        
        return@withContext videos ?: emptyList()
    }

    suspend fun createUserIfNotExists(userId: String, defaultUser: User): User? = withContext(Dispatchers.IO) {
        try {
            var existingUser: User? = null
            firestore.runTransaction { transaction ->
                val docRef = firestore.collection("users").document(userId)
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    transaction.set(docRef, defaultUser)
                } else {
                    existingUser = snapshot.toObject(User::class.java)?.copy(id = snapshot.id)
                }
            }.await()
            return@withContext existingUser ?: defaultUser
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun sendNotification(
        toUserId: String,
        fromUserId: String,
        fromUsername: String,
        fromUserPhoto: String,
        type: String,
        videoId: String? = null,
        message: String = ""
    ) {
        if (toUserId == fromUserId) return // Don't notify oneself
        try {
            val notificationRef = firestore.collection("notifications").document()
            val notification = com.example.models.Notification(
                id = notificationRef.id,
                toUserId = toUserId,
                fromUserId = fromUserId,
                fromUsername = fromUsername,
                fromUserPhoto = fromUserPhoto,
                type = type,
                videoId = videoId,
                message = message,
                timestamp = System.currentTimeMillis()
            )
            notificationRef.set(notification).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchNotifications(userId: String): List<com.example.models.Notification> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("toUserId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(com.example.models.Notification::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun markNotificationAsRead(notificationId: String) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("notifications").document(notificationId).update("isRead", true).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun toggleLike(videoId: String, userId: String) = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("videos").document(videoId)
            var videoOwnerId = ""
            var liked = false
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val video = snapshot.toObject(VideoItem::class.java) ?: return@runTransaction
                videoOwnerId = video.userId
                
                val likedBy = video.likedBy.toMutableList()
                if (likedBy.contains(userId)) {
                    likedBy.remove(userId)
                    liked = false
                } else {
                    likedBy.add(userId)
                    liked = true
                }
                
                transaction.update(docRef, "likedBy", likedBy)
                transaction.update(docRef, "likes", likedBy.size)
            }.await()
            
            if (liked && videoOwnerId.isNotEmpty() && videoOwnerId != userId) {
                // Fetch user to get name/photo
                val user = getUserProfile(userId)
                if (user != null) {
                    sendNotification(
                        toUserId = videoOwnerId,
                        fromUserId = userId,
                        fromUsername = user.username,
                        fromUserPhoto = user.profilePhoto,
                        type = "like",
                        videoId = videoId,
                        message = "liked your video"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchComments(videoId: String): List<Comment> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("videos").document(videoId).collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Comment::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addComment(videoId: String, comment: Comment): Comment? = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("videos").document(videoId).collection("comments").document()
            val newComment = comment.copy(id = docRef.id)
            docRef.set(newComment).await()
            
            firestore.collection("videos").document(videoId).update("comments", com.google.firebase.firestore.FieldValue.increment(1)).await()
            
            // Notify video owner
            val videoSnapshot = firestore.collection("videos").document(videoId).get().await()
            val videoOwnerId = videoSnapshot.toObject(VideoItem::class.java)?.userId
            
            if (videoOwnerId != null && videoOwnerId != comment.userId) {
                sendNotification(
                    toUserId = videoOwnerId,
                    fromUserId = comment.userId,
                    fromUsername = comment.username,
                    fromUserPhoto = comment.userPhoto,
                    type = "comment",
                    videoId = videoId,
                    message = "commented on your video: ${comment.text}"
                )
            }
            
            return@withContext newComment
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun deleteComment(videoId: String, commentId: String) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("videos").document(videoId).collection("comments").document(commentId).delete().await()
            
            firestore.collection("videos").document(videoId).update("comments", com.google.firebase.firestore.FieldValue.increment(-1)).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getUserProfile(userId: String): User? = withContext(Dispatchers.IO) {
        var retryCount = 0
        var user: User? = null
        
        try {
            firestore.enableNetwork().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        while (retryCount < 3) {
            try {
                val snapshot = firestore.collection("users").document(userId).get().await()
                if (snapshot.exists()) {
                    user = snapshot.toObject(User::class.java)?.copy(id = snapshot.id)
                    break
                } else {
                    return@withContext null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                retryCount++
                kotlinx.coroutines.delay(1000)
            }
        }
        
        if (user == null && retryCount >= 3) {
            throw Exception("Failed to get document because the client is offline or network error.")
        }
        
        return@withContext user
    }

    suspend fun toggleSaveVideo(userId: String, videoId: String) = withContext(Dispatchers.IO) {
        try {
            val userRef = firestore.collection("users").document(userId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val user = snapshot.toObject(User::class.java) ?: return@runTransaction
                
                val savedVideos = user.savedVideos.toMutableList()
                if (savedVideos.contains(videoId)) {
                    savedVideos.remove(videoId)
                } else {
                    savedVideos.add(videoId)
                }
                
                transaction.update(userRef, "savedVideos", savedVideos)
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun searchUsers(query: String): List<User> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users").get().await()
            val lowerQuery = query.lowercase()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }.filter { 
                it.username.lowercase().contains(lowerQuery) || it.name.lowercase().contains(lowerQuery)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun searchVideos(query: String): List<VideoItem> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("videos").get().await()
            val lowerQuery = query.lowercase()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(VideoItem::class.java)?.copy(id = doc.id)
            }.filter { 
                it.caption.lowercase().contains(lowerQuery)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun toggleFollow(currentUserId: String, targetUserId: String) = withContext(Dispatchers.IO) {
        try {
            val currentUserRef = firestore.collection("users").document(currentUserId)
            val targetUserRef = firestore.collection("users").document(targetUserId)
            var followed = false
            var followedUser: User? = null
            var currentUserObj: User? = null
            
            firestore.runTransaction { transaction ->
                val currentUserSnap = transaction.get(currentUserRef)
                val targetUserSnap = transaction.get(targetUserRef)
                
                currentUserObj = currentUserSnap.toObject(User::class.java)
                val currentUser = currentUserObj ?: return@runTransaction
                val targetUser = targetUserSnap.toObject(User::class.java) ?: return@runTransaction
                followedUser = targetUser
                
                val followingList = currentUser.followingList.toMutableList()
                val followersList = targetUser.followersList.toMutableList()
                
                if (followingList.contains(targetUserId)) {
                    followingList.remove(targetUserId)
                    followersList.remove(currentUserId)
                    followed = false
                } else {
                    followingList.add(targetUserId)
                    followersList.add(currentUserId)
                    followed = true
                }
                
                transaction.update(currentUserRef, "followingList", followingList)
                transaction.update(currentUserRef, "following", followingList.size)
                
                transaction.update(targetUserRef, "followersList", followersList)
                transaction.update(targetUserRef, "followers", followersList.size)
            }.await()
            
            if (followed && currentUserObj != null) {
                sendNotification(
                    toUserId = targetUserId,
                    fromUserId = currentUserId,
                    fromUsername = currentUserObj!!.username,
                    fromUserPhoto = currentUserObj!!.profilePhoto,
                    type = "follow",
                    message = "started following you"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
