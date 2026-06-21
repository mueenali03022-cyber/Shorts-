package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.AppViewModel
import com.example.ui.theme.PureBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashtagScreen(
    hashtagName: String, // includes the #
    appViewModel: AppViewModel,
    navController: NavController
) {
    val videos by appViewModel.videos.collectAsState()
    val hashtagVideos = videos.filter { it.hashtags.contains(hashtagName.lowercase()) || it.caption.lowercase().contains(hashtagName.lowercase()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(hashtagName, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureBlack)
            )
        },
        containerColor = PureBlack
    ) { paddingValues ->
        if (hashtagVideos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No videos found with $hashtagName", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(paddingValues).fillMaxSize()
            ) {
                itemsIndexed(hashtagVideos) { index, video ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.75f)
                            .background(Color.DarkGray)
                            .clickable {
                                // Since we don't have a specific feed screen for subsets of videos, we could pass it somehow.
                                // But simply navigating to main feed starting at the ID is tricky if it's the global list.
                                // Let's try navigating to a special feed route if we want to show hashtag videos.
                                // For now, let's just navigate to the video directly if we had a route for it.
                                // I will navigate to a new route: "hashtag_feed/${hashtagName}/${index}"
                                navController.navigate("hashtag_feed/${hashtagName.replace("#", "")}/${index}")
                            }
                    ) {
                        AsyncImage(
                            model = video.videoUrl, // Coil handles video frames automatically if configured, otherwise shows placeholder. Wait, AsyncImage doesn't automatically show video thumbnails unless video frame fetcher is added. For now, it might be blank, but it's consistent with existing profile grid.
                            contentDescription = "Video Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
