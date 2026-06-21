package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.models.User
import com.example.models.VideoItem
import com.example.ui.theme.DarkGray

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

@Composable
fun ProfileScreen(
    user: User, 
    userVideos: List<VideoItem>, 
    isCurrentUser: Boolean,
    appViewModel: com.example.AppViewModel,
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val currentUser by appViewModel.currentUser.collectAsState()
    val isFollowing = currentUser?.followingList?.contains(user.id) ?: false
    var isEditing by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

    if (isEditing && isCurrentUser) {
        EditProfileView(
            user = currentUser ?: user,
            appViewModel = appViewModel,
            onCancel = { isEditing = false },
            onSave = { isEditing = false }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isCurrentUser) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
                Text(
                    text = user.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (isCurrentUser) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Settings", tint = Color.White)
                    }
                } else {
                    IconButton(onClick = { showBlockDialog = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                }
            }
            
            if (showBlockDialog) {
                AlertDialog(
                    onDismissRequest = { showBlockDialog = false },
                    title = { Text("Block User") },
                    text = { Text("Are you sure you want to block ${user.name}? You will no longer see their content.") },
                    confirmButton = {
                        TextButton(onClick = { 
                            showBlockDialog = false
                            appViewModel.blockUser(user.id)
                            onBack()
                        }) { Text("Block", color = Color.Red) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
                    }
                )
            }

            // Profile Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = user.profilePhoto,
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(DarkGray)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "@${user.username}", color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn(count = user.following.toString(), label = "Following")
                    StatColumn(count = user.followers.toString(), label = "Followers")
                    StatColumn(count = user.likes.toString(), label = "Likes")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 24.dp)) {
                    if (isCurrentUser) {
                        Button(
                            onClick = { isEditing = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Edit Profile", color = Color.White)
                        }
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Add Friends", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = { appViewModel.toggleFollow(user.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isFollowing) DarkGray else com.example.ui.theme.AccentRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isFollowing) "Unfollow" else "Follow", color = Color.White)
                        }
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Message", color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = user.bio, color = Color.White, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(modifier = Modifier.height(24.dp))
            }

            var selectedTab by remember { mutableStateOf(0) }
            val tabs = if (isCurrentUser) listOf("Videos", "Saved") else listOf("Videos")

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Black,
                contentColor = Color.White
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Videos Grid
            val videosFromGlobal by appViewModel.videos.collectAsState()
            val savedVideosList = videosFromGlobal.filter { user.savedVideos.contains(it.id) }
            val displayVideos = if (selectedTab == 0) userVideos else savedVideosList

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayVideos) { video ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.75f)
                            .background(DarkGray)
                            .clickable { onVideoClick(video.id) }
                    ) {
                        // Thumbnail placeholder since we only have urls
                        AsyncImage(
                             model = video.videoUrl.replace(".mp4", ".jpg").replace(".mkv", ".jpg").replace(".mov", ".jpg"),
                             contentDescription = "Thumbnail",
                             contentScale = ContentScale.Crop,
                             modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileView(
    user: User,
    appViewModel: com.example.AppViewModel,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf(user.username.removePrefix("@")) }
    var bio by remember { mutableStateOf(user.bio) }
    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        profilePhotoUri = uri
    }
    
    val isLoading by appViewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel, enabled = !isLoading) {
                Text("Cancel", color = Color.White)
            }
            Text(
                text = "Edit Profile",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            TextButton(
                onClick = { 
                    appViewModel.updateUserProfile(username, bio, profilePhotoUri, context)
                    onSave() 
                },
                enabled = !isLoading
            ) {
                Text("Save", color = com.example.ui.theme.AccentRed)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = profilePhotoUri ?: user.profilePhoto,
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(DarkGray)
                )
                if (isLoading) {
                    CircularProgressIndicator(color = com.example.ui.theme.AccentRed)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { launcher.launch("image/*") }) {
                Text("Change Photo", color = com.example.ui.theme.AccentRed)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username", color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = com.example.ui.theme.AccentRed,
                unfocusedBorderColor = DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio", color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(120.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = com.example.ui.theme.AccentRed,
                unfocusedBorderColor = DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            maxLines = 5
        )
    }
}

@Composable
fun StatColumn(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
    }
}
