package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.models.VideoItem
import com.example.ui.components.VideoPlayer
import com.example.ui.theme.AccentRed
import com.example.ui.theme.PureBlack

import com.example.AppViewModel // Assuming it's in com.example
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Send

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    videos: List<VideoItem>,
    currentUserId: String,
    onLikeToggle: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit = {},
    appViewModel: com.example.AppViewModel,
    initialPage: Int = 0
) {
    if (videos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(PureBlack), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No videos yet", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Be the first to upload!", color = Color.Gray, fontSize = 16.sp)
            }
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = if (initialPage in videos.indices) initialPage else 0,
        pageCount = { videos.size }
    )
    var showCommentsForVideo by remember { mutableStateOf<String?>(null) }
    var showReportDialogForVideo by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val video = videos[page]
        val isPlaying = pagerState.currentPage == page && showCommentsForVideo == null

        Box(modifier = Modifier.fillMaxSize().background(PureBlack)) {
            VideoPlayer(
                videoUrl = video.videoUrl,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize()
            )
            
            // Overlays
            FeedOverlay(
                video = video,
                currentUserId = currentUserId,
                onLikeToggle = { onLikeToggle(video.id) },
                onCommentClick = { 
                    appViewModel.loadComments(video.id)
                    showCommentsForVideo = video.id 
                },
                onProfileClick = {
                    onProfileClick(video.userId)
                },
                isSaved = appViewModel.currentUser.collectAsState().value?.savedVideos?.contains(video.id) == true,
                onSaveToggle = { appViewModel.toggleSaveVideo(video.id) },
                onReportClick = { showReportDialogForVideo = video.id },
                onHashtagClick = onHashtagClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showReportDialogForVideo != null) {
        AlertDialog(
            onDismissRequest = { showReportDialogForVideo = null },
            title = { Text("Report Content") },
            text = { Text("Do you want to report this video for inappropriate content? This action will remove the video from your feed and notify administrators.") },
            confirmButton = {
                TextButton(onClick = { 
                    appViewModel.reportVideo(showReportDialogForVideo!!)
                    showReportDialogForVideo = null
                }) { Text("Report", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialogForVideo = null }) { Text("Cancel") }
            }
        )
    }

    if (showCommentsForVideo != null) {
        ModalBottomSheet(
            onDismissRequest = { showCommentsForVideo = null },
            sheetState = sheetState,
            containerColor = Color.DarkGray
        ) {
            CommentsSheet(
                videoId = showCommentsForVideo!!,
                currentUserId = currentUserId,
                appViewModel = appViewModel
            )
        }
    }
}

@Composable
fun CommentsSheet(
    videoId: String,
    currentUserId: String,
    appViewModel: com.example.AppViewModel
) {
    val comments by appViewModel.currentComments.collectAsState()
    var commentText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
            .padding(16.dp)
    ) {
        Text("Comments", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(comments) { comment ->
                Row(verticalAlignment = Alignment.Top) {
                    AsyncImage(
                        model = comment.userPhoto,
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("@${comment.username}", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(comment.text, color = Color.White, fontSize = 14.sp)
                    }
                    if (comment.userId == currentUserId) {
                        IconButton(onClick = { appViewModel.deleteComment(videoId, comment.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = commentText,
                onValueChange = { commentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a comment...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.DarkGray,
                    unfocusedContainerColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = CircleShape
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (commentText.isNotBlank()) {
                        appViewModel.addComment(videoId, commentText)
                        commentText = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AccentRed)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}
@Composable
fun FeedOverlay(
    video: VideoItem,
    currentUserId: String,
    onLikeToggle: () -> Unit,
    onCommentClick: () -> Unit,
    onProfileClick: () -> Unit,
    isSaved: Boolean,
    onSaveToggle: () -> Unit,
    onReportClick: () -> Unit,
    onHashtagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Bottom Left details
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "@${video.username}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.clickable { onProfileClick() }
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val annotatedCaption = buildAnnotatedString {
                val words = video.caption.split(" ")
                for (word in words) {
                    if (word.startsWith("#")) {
                        val linkAnnotation = androidx.compose.ui.text.LinkAnnotation.Clickable(word) {
                            onHashtagClick(word)
                        }
                        pushLink(linkAnnotation)
                        withStyle(style = androidx.compose.ui.text.SpanStyle(color = AccentRed, fontWeight = FontWeight.Bold)) {
                            append("$word ")
                        }
                        pop()
                    } else {
                        append("$word ")
                    }
                }
            }
            
            Text(
                text = annotatedCaption,
                style = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = video.music,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        // Right side interaction buttons
        Column(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Image
            Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.clickable { onProfileClick() }) {
                AsyncImage(
                    model = video.userPhoto,
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
                // Add Icon Overlay
                Box(
                    modifier = Modifier
                        .offset(y = 8.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(AccentRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Like
            val isLiked = video.likedBy.contains(currentUserId)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onLikeToggle() }) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Like",
                    tint = if (isLiked) AccentRed else Color.White,
                    modifier = Modifier.size(32.dp).padding(4.dp)
                )
                Text("${video.likes}", color = Color.White, fontSize = 12.sp)
            }

            // Comment
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onCommentClick() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Message,
                    contentDescription = "Comment",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp).padding(4.dp)
                )
                Text("${video.comments}", color = Color.White, fontSize = 12.sp)
            }

            // Share
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Check out this video! ${video.videoUrl}")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share video via"))
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp).padding(4.dp)
                )
                Text("${video.shares}", color = Color.White, fontSize = 12.sp)
            }
            
            // Save/Bookmark
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSaveToggle() }) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Save",
                    tint = if (isSaved) AccentRed else Color.White,
                    modifier = Modifier.size(32.dp).padding(4.dp)
                )
            }
            
            // Report
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onReportClick() }) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Report",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp).padding(4.dp)
                )
            }
        }
    }
}
