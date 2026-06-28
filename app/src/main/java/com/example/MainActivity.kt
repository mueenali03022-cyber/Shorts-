package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.LoginScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.BottomNavItem
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PureBlack

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
          val appViewModel: AppViewModel = viewModel()
          val isLoading by appViewModel.isLoading.collectAsState()
          val errorMessage by appViewModel.errorMessage.collectAsState()
          val currentUser by appViewModel.currentUser.collectAsState()
          val videos by appViewModel.videos.collectAsState()
          val unreadCount by appViewModel.unreadCount.collectAsState()

          // Check for authenticated user before showing main app content
          if (currentUser == null) {
              LoginScreen(
                  isLoading = isLoading,
                  errorMessage = errorMessage,
                  onLoginClick = { email, pass ->
                      appViewModel.signIn(email, pass)
                  },
                  onSignupClick = { email, pass ->
                      appViewModel.signUp(email, pass)
                  }
              )
          } else {
              val navController = rememberNavController()
              Scaffold(
                  modifier = Modifier.fillMaxSize(),
                  bottomBar = { AppBottomNavigation(navController, unreadCount) },
                  containerColor = PureBlack
              ) { innerPadding ->
                  NavHost(
                      navController = navController,
                      startDestination = BottomNavItem.Upload.route,
                      modifier = Modifier.padding(innerPadding).fillMaxSize()
                  ) {
                      composable(BottomNavItem.Home.route) {
                          FeedScreen(
                              videos = videos,
                              currentUserId = currentUser!!.id,
                              onLikeToggle = { videoId ->
                                  appViewModel.toggleLike(videoId)
                              },
                              onProfileClick = { userId ->
                                  if (userId == currentUser!!.id) {
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
                      composable(BottomNavItem.Discover.route) {
                          com.example.ui.screens.SearchScreen(
                              appViewModel = appViewModel,
                              navController = navController
                          )
                      }
                      composable(BottomNavItem.Upload.route) {
                          val uploadState by appViewModel.uploadState.collectAsState()
                          com.example.ui.screens.UploadScreen(
                              navController = navController, 
                              uploadState = uploadState,
                              onUploadVideo = { ctx, uri, caption, privacy, allowComments, allowDownloads ->
                                  appViewModel.uploadVideo(ctx, uri, caption, privacy, allowComments, allowDownloads)
                              }
                          )
                      }
                      composable(BottomNavItem.Inbox.route) {
                          com.example.ui.screens.NotificationsScreen(
                              appViewModel = appViewModel,
                              navController = navController
                          )
                      }
                      composable(BottomNavItem.Profile.route) {
                          ProfileScreen(
                              user = currentUser!!,
                              userVideos = videos.filter { it.userId == currentUser!!.id },
                              isCurrentUser = true,
                              appViewModel = appViewModel,
                              onBack = { navController.popBackStack() },
                              onVideoClick = { videoId ->
                                  navController.navigate("video_player/$videoId")
                              },
                              onSettingsClick = {
                                  navController.navigate("settings")
                              }
                          )
                      }
                      composable("user_profile/{userId}") { backStackEntry ->
                          val userId = backStackEntry.arguments?.getString("userId")
                          val targetUser = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.models.User?>(null) }
                          
                          androidx.compose.runtime.LaunchedEffect(userId) {
                              if (userId != null) {
                                  targetUser.value = appViewModel.getUserProfile(userId)
                              }
                          }
                          
                          if (targetUser.value != null) {
                              ProfileScreen(
                                  user = targetUser.value!!,
                                  userVideos = videos.filter { it.userId == targetUser.value!!.id },
                                  isCurrentUser = targetUser.value!!.id == currentUser!!.id,
                                  appViewModel = appViewModel,
                                  onBack = { navController.popBackStack() },
                                  onVideoClick = { videoId ->
                                      navController.navigate("video_player/$videoId")
                                  },
                                  onSettingsClick = {
                                      navController.navigate("settings")
                                  }
                              )
                          } else {
                              Box(modifier = Modifier.fillMaxSize().background(PureBlack), contentAlignment = Alignment.Center) {
                                  androidx.compose.material3.CircularProgressIndicator()
                              }
                          }
                      }
                      
                      composable("settings") {
                          com.example.ui.screens.SettingsScreen(navController, appViewModel)
                      }
                      composable("privacy_policy") {
                          com.example.ui.screens.PrivacyPolicyScreen(navController)
                      }
                      composable("tos") {
                          com.example.ui.screens.TOSScreen(navController)
                      }
                      composable("safety_settings") {
                          com.example.ui.screens.SafetySettingsScreen(navController)
                      }

                      composable("video_player/{videoId}") { backStackEntry ->
                          val videoId = backStackEntry.arguments?.getString("videoId")
                          val initialIndex = videos.indexOfFirst { it.id == videoId }.takeIf { it >= 0 } ?: 0
                          
                          FeedScreen(
                              videos = videos,
                              currentUserId = currentUser!!.id,
                              onLikeToggle = { vId ->
                                  appViewModel.toggleLike(vId)
                              },
                              onProfileClick = { uId ->
                                  if (uId == currentUser!!.id) {
                                      navController.navigate(BottomNavItem.Profile.route)
                                  } else {
                                      navController.navigate("user_profile/$uId")
                                  }
                              },
                              appViewModel = appViewModel,
                              initialPage = initialIndex,
                              onHashtagClick = { hashtag ->
                                  navController.navigate("hashtag/${hashtag.replace("#", "")}")
                              }
                          )
                      }
                      composable("hashtag/{hashtag}") { backStackEntry ->
                          val hashtag = backStackEntry.arguments?.getString("hashtag") ?: ""
                          com.example.ui.screens.HashtagScreen(
                              hashtagName = "#$hashtag",
                              appViewModel = appViewModel,
                              navController = navController
                          )
                      }
                      composable("hashtag_feed/{hashtag}/{index}") { backStackEntry ->
                          val hashtagStr = backStackEntry.arguments?.getString("hashtag") ?: ""
                          val index = backStackEntry.arguments?.getString("index")?.toIntOrNull() ?: 0
                          val hashtag = "#$hashtagStr".lowercase()
                          val hashtagVideos = videos.filter { it.hashtags.contains(hashtag) || it.caption.lowercase().contains(hashtag) }
                          
                          FeedScreen(
                              videos = hashtagVideos,
                              currentUserId = currentUser!!.id,
                              onLikeToggle = { vId ->
                                  appViewModel.toggleLike(vId)
                              },
                              onProfileClick = { uId ->
                                  if (uId == currentUser!!.id) {
                                      navController.navigate(BottomNavItem.Profile.route)
                                  } else {
                                      navController.navigate("user_profile/$uId")
                                  }
                              },
                              appViewModel = appViewModel,
                              initialPage = index,
                              onHashtagClick = { h ->
                                  navController.navigate("hashtag/${h.replace("#", "")}")
                              }
                          )
                      }
                  }
              }
          }
      }
    }
  }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize().background(PureBlack), contentAlignment = Alignment.Center) {
        Text(text = title, color = Color.White)
    }
}
