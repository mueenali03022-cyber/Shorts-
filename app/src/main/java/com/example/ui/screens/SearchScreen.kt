package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.AppViewModel
import com.example.models.User
import com.example.models.VideoItem
import com.example.ui.components.BottomNavItem
import com.example.ui.theme.DarkGray
import com.example.ui.theme.PureBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    appViewModel: AppViewModel,
    navController: NavController
) {
    var query by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchedUsers by appViewModel.searchedUsers.collectAsState()
    val searchedVideos by appViewModel.searchedVideos.collectAsState()
    val isSearching by appViewModel.isSearching.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Users, 1: Videos
    val tabs = listOf("Users", "Videos")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                appViewModel.performSearch(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = DarkGray,
                cursorColor = Color.White,
                focusedContainerColor = DarkGray,
                unfocusedContainerColor = DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { keyboardController?.hide() }
            ),
            shape = MaterialTheme.shapes.extraLarge
        )

        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            if (query.isNotBlank()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PureBlack,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color.White
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                if (selectedTab == 0) {
                    // Users List
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxSize().weight(1f)
                    ) {
                        if (searchedUsers.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No users found", color = Color.Gray)
                                }
                            }
                        }
                        items(searchedUsers) { user ->
                            UserListItem(
                                user = user,
                                onClick = {
                                    val currentUser = appViewModel.currentUser.value
                                    if (currentUser?.id == user.id) {
                                        navController.navigate(BottomNavItem.Profile.route)
                                    } else {
                                        navController.navigate("user_profile/${user.id}")
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // Videos Feed Array
                    if (searchedVideos.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp).weight(1f), contentAlignment = Alignment.Center) {
                            Text("No videos found", color = Color.Gray)
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                            com.example.ui.screens.FeedScreen(
                                videos = searchedVideos,
                                currentUserId = appViewModel.currentUser.value?.id ?: "",
                                onLikeToggle = { appViewModel.toggleLike(it) },
                                onProfileClick = { userId ->
                                    val currentUser = appViewModel.currentUser.value
                                    if (currentUser?.id == userId) {
                                        navController.navigate(BottomNavItem.Profile.route)
                                    } else {
                                        navController.navigate("user_profile/$userId")
                                    }
                                },
                                appViewModel = appViewModel,
                                onHashtagClick = { hashtag ->
                                    navController.navigate("hashtag/${hashtag.replace("#", "")}")
                                }
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Search for users or videos", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun UserListItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.profilePhoto,
            contentDescription = "Profile Photo",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(DarkGray),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(user.username, color = Color.White, fontWeight = FontWeight.Bold)
            if (user.name.isNotBlank()) {
                Text(user.name, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
