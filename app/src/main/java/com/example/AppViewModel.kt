package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirebaseManager
import com.example.models.User
import com.example.models.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.tasks.await

import com.example.models.UploadState

import com.example.models.Comment

class AppViewModel : ViewModel() {
    private val firebaseManager = FirebaseManager()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos = _videos.asStateFlow()

    private val _currentComments = MutableStateFlow<List<Comment>>(emptyList())
    val currentComments = _currentComments.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState = _uploadState.asStateFlow()

    private val _searchedUsers = MutableStateFlow<List<User>>(emptyList())
    val searchedUsers = _searchedUsers.asStateFlow()

    private val _searchedVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val searchedVideos = _searchedVideos.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        val user = firebaseManager.auth.currentUser
        if (user != null) {
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    val userProfile = firebaseManager.getUserProfile(user.uid)
                    if (userProfile != null) {
                        _currentUser.value = userProfile
                    } else {
                        // Create default profile using transaction to avoid accidental overwrites
                        val newUser = User(
                            id = user.uid,
                            name = user.displayName ?: user.email?.substringBefore("@") ?: "User",
                            username = "user" + user.uid.take(6).lowercase(),
                            profilePhoto = "https://i.pravatar.cc/150?u=${user.uid}"
                        )
                        val safeUser = firebaseManager.createUserIfNotExists(user.uid, newUser)
                        if (safeUser != null) {
                            _currentUser.value = safeUser
                        } else {
                            // If transaction fails, just load whatever we can from getUserProfile or use new
                            _currentUser.value = firebaseManager.getUserProfile(user.uid) ?: newUser
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (_currentUser.value == null) {
                        // Keep any existing cached user data (if _currentUser was already set, we don't clear it or show fatal error)
                        // If it's still null, we might not have any user data at all to display, but at least we don't overwrite it.
                        _errorMessage.value = "Failed to load profile: ${e.message}"
                    }
                } finally {
                    _isLoading.value = false
                }
                
                // Load videos sequentially after profile, without blocking UI
                loadVideos()
                loadNotifications()
            }
        } else {
            _isLoading.value = false
        }
    }

    private val _notifications = MutableStateFlow<List<com.example.models.Notification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()

    fun loadNotifications() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                val fetchedNotifications = firebaseManager.fetchNotifications(user.id)
                _notifications.value = fetchedNotifications
                _unreadCount.value = fetchedNotifications.count { !it.isRead }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                firebaseManager.markNotificationAsRead(notificationId)
                val updatedNotifications = _notifications.value.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
                _notifications.value = updatedNotifications
                _unreadCount.value = updatedNotifications.count { !it.isRead }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadVideos() {
        viewModelScope.launch {
            try {
                val fetchedVideos = firebaseManager.fetchVideos()
                _videos.value = fetchedVideos
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun signIn(email: String, pass: String) {
        _isLoading.value = true
        _errorMessage.value = null
        firebaseManager.auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkAuthStatus()
                } else {
                    _isLoading.value = false
                    _errorMessage.value = task.exception?.localizedMessage ?: "Login failed"
                    task.exception?.printStackTrace()
                }
            }
    }

    fun signUp(email: String, pass: String) {
        _isLoading.value = true
        _errorMessage.value = null
        firebaseManager.auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkAuthStatus()
                } else {
                    _isLoading.value = false
                    _errorMessage.value = task.exception?.localizedMessage ?: "Signup failed"
                    task.exception?.printStackTrace()
                }
            }
    }

    fun updateUserProfile(username: String, bio: String, photoUri: android.net.Uri?, context: android.content.Context) {
        val user = _currentUser.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            firebaseManager.updateUserProfile(user.id, username, bio, photoUri, context)
            
            try {
                // Refresh current user profile
                val updatedUser = firebaseManager.getUserProfile(user.id)
                if (updatedUser != null) {
                    _currentUser.value = updatedUser
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        firebaseManager.auth.signOut()
        _currentUser.value = null
    }

    fun deleteAccount() {
        val user = firebaseManager.auth.currentUser ?: return
        viewModelScope.launch {
            try {
                // In a real app we'd also delete the Firebase user doc and uploaded videos
                user.delete().await()
                _currentUser.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete account: ${e.message}"
            }
        }
    }

    fun blockUser(userId: String) {
        // Implement block user logic (add to blocked users list in DB)
        // For now just hide the videos locally
        _videos.value = _videos.value.filter { it.userId != userId }
    }

    fun reportVideo(videoId: String, reason: String = "Inappropriate content") {
        // Implement report logic to database
        // For now, give immediate feedback by removing it locally
        _videos.value = _videos.value.filter { it.id != videoId }
    }

    fun signInAnonymously() {
        _isLoading.value = true
        _errorMessage.value = null
        firebaseManager.auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkAuthStatus()
                } else {
                    _isLoading.value = false
                    _errorMessage.value = task.exception?.localizedMessage ?: "Login failed"
                    task.exception?.printStackTrace()
                }
            }
    }

    fun uploadVideo(context: android.content.Context, uri: android.net.Uri, caption: String, privacy: String = "Public", allowComments: Boolean = true, allowDownloads: Boolean = true) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            firebaseManager.uploadVideo(context, user, uri, caption, privacy, allowComments, allowDownloads) { state ->
                _uploadState.value = state
                if (state is UploadState.Success) {
                    _uploadState.value = UploadState.Idle
                    loadVideos()
                }
            }
        }
    }

    fun toggleSaveVideo(videoId: String) {
        val user = _currentUser.value ?: return
        
        val newSavedVideos = user.savedVideos.toMutableList()
        if (newSavedVideos.contains(videoId)) {
            newSavedVideos.remove(videoId)
        } else {
            newSavedVideos.add(videoId)
        }
        
        _currentUser.value = user.copy(savedVideos = newSavedVideos)
        
        viewModelScope.launch {
            firebaseManager.toggleSaveVideo(user.id, videoId)
        }
    }

    fun toggleLike(videoId: String) {
        val user = _currentUser.value ?: return
        
        _videos.value = _videos.value.map { video ->
            if (video.id == videoId) {
                val likedBy = video.likedBy.toMutableList()
                if (likedBy.contains(user.id)) {
                    likedBy.remove(user.id)
                } else {
                    likedBy.add(user.id)
                }
                video.copy(likedBy = likedBy, likes = likedBy.size)
            } else {
                video
            }
        }

        viewModelScope.launch {
            firebaseManager.toggleLike(videoId, user.id)
        }
    }

    fun loadComments(videoId: String) {
        viewModelScope.launch {
            val comments = firebaseManager.fetchComments(videoId)
            _currentComments.value = comments
            _videos.value = _videos.value.map { v ->
                if (v.id == videoId) v.copy(comments = comments.size) else v
            }
        }
    }

    fun addComment(videoId: String, text: String) {
        val user = _currentUser.value ?: return
        val tempId = System.currentTimeMillis().toString()
        val comment = Comment(
            id = tempId,
            videoId = videoId,
            userId = user.id,
            username = user.username,
            userPhoto = user.profilePhoto,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        
        // Optimistic update
        val updatedComments = _currentComments.value.toMutableList()
        updatedComments.add(0, comment)
        _currentComments.value = updatedComments

        _videos.value = _videos.value.map { v ->
            if (v.id == videoId) v.copy(comments = v.comments + 1) else v
        }

        viewModelScope.launch {
            val addedComment = firebaseManager.addComment(videoId, comment)
            if (addedComment != null) {
                val newList = _currentComments.value.toMutableList()
                val index = newList.indexOfFirst { it.id == tempId }
                if (index != -1) {
                    newList[index] = addedComment
                    _currentComments.value = newList
                }
            } else {
                // Rollback
                val currentList = _currentComments.value.toMutableList()
                currentList.removeAll { it.id == tempId }
                _currentComments.value = currentList
                _videos.value = _videos.value.map { v ->
                    if (v.id == videoId && v.comments > 0) v.copy(comments = v.comments - 1) else v
                }
            }
        }
    }

    fun deleteComment(videoId: String, commentId: String) {
        val currentList = _currentComments.value.toMutableList()
        currentList.removeAll { it.id == commentId }
        _currentComments.value = currentList
        
        _videos.value = _videos.value.map { v ->
            if (v.id == videoId && v.comments > 0) v.copy(comments = v.comments - 1) else v
        }

        viewModelScope.launch {
            firebaseManager.deleteComment(videoId, commentId)
        }
    }

    suspend fun getUserProfile(userId: String): User? {
        return firebaseManager.getUserProfile(userId)
    }

    fun performSearch(query: String) {
        if (query.isBlank()) {
            _searchedUsers.value = emptyList()
            _searchedVideos.value = emptyList()
            return
        }
        
        _isSearching.value = true
        viewModelScope.launch {
            val users = firebaseManager.searchUsers(query)
            val vids = firebaseManager.searchVideos(query)
            _searchedUsers.value = users
            _searchedVideos.value = vids
            _isSearching.value = false
        }
    }

    fun toggleFollow(targetUserId: String) {
        val currentUserVal = _currentUser.value ?: return
        if (currentUserVal.id == targetUserId) return
        
        val followingList = currentUserVal.followingList.toMutableList()
        val isFollowing = followingList.contains(targetUserId)
        
        if (isFollowing) {
            followingList.remove(targetUserId)
        } else {
            followingList.add(targetUserId)
        }
        
        _currentUser.value = currentUserVal.copy(
            followingList = followingList,
            following = followingList.size
        )
        
        viewModelScope.launch {
            firebaseManager.toggleFollow(currentUserVal.id, targetUserId)
        }
    }
}
