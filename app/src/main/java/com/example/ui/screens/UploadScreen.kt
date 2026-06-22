package com.example.ui.screens

import coil.compose.AsyncImage
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.example.utils.CameraFilter
import com.example.utils.FilterSystem
import com.example.utils.CameraEffect
import com.example.utils.EffectsSystem
import androidx.compose.animation.core.animateFloat
import android.Manifest
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.models.UploadState
import com.example.ui.components.VideoPlayer
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DarkGray
import com.example.ui.theme.PureBlack
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor

enum class UploadStep {
    Camera, Preview, Details
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    navController: NavController,
    uploadState: UploadState,
    onUploadVideo: (Context, Uri, String, String, Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    var uploadStep by remember { mutableStateOf(UploadStep.Camera) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    
    // Privacy and Comments toggles
    var privacy by remember { mutableStateOf("Public") }
    var allowComments by remember { mutableStateOf(true) }

    var allowDownloads by remember { mutableStateOf(true) }

    LaunchedEffect(uploadState) {
        if (uploadState is UploadState.Success) {
            navController.navigateUp()
        }
    }

    when (uploadStep) {
        UploadStep.Camera -> {
            val cameraPermissions = rememberMultiplePermissionsState(
                listOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )

            if (cameraPermissions.allPermissionsGranted) {
                CameraPreviewScreen(
                    onVideoRecorded = { uri ->
                        selectedVideoUri = uri
                        uploadStep = UploadStep.Preview
                    },
                    onClose = { navController.navigateUp() }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PureBlack)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Camera & Microphone Access Required", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("To let you capture and record videos for your profile, this app needs access to your camera and microphone. Your recordings are only uploaded to our servers when you choose to publish them.", color = Color.LightGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { cameraPermissions.launchMultiplePermissionRequest() }, colors = ButtonDefaults.buttonColors(containerColor = AccentRed)) {
                        Text("Grant Permissions", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { navController.navigateUp() }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
        UploadStep.Preview -> {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                selectedVideoUri?.let { uri ->
                    VideoPlayer(videoUrl = uri.toString(), isPlaying = true)
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { uploadStep = UploadStep.Camera }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Button(onClick = { uploadStep = UploadStep.Details }, colors = ButtonDefaults.buttonColors(containerColor = AccentRed)) {
                        Text("Next", color = Color.White)
                    }
                }
                
                // Studio Edit placehorders
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                ) {
                    IconButton(onClick = { /* Trim */ }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ContentCut, contentDescription = "Edit", tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    IconButton(onClick = { /* Text */ }) {
                        Icon(Icons.Default.TextFields, contentDescription = "Text", tint = Color.White)
                    }
                    Spacer(Modifier.height(16.dp))
                    IconButton(onClick = { /* Stickers */ }) {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = "Stickers", tint = Color.White)
                    }
                    Spacer(Modifier.height(16.dp))
                    IconButton(onClick = { /* Voiceover */ }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voiceover", tint = Color.White)
                    }
                }
            }
        }
        UploadStep.Details -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Post", color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = { uploadStep = UploadStep.Preview }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = PureBlack)
                    )
                },
                containerColor = PureBlack
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Caption (include #hashtags and @mentions)", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentRed,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 5
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Location", color = Color.White)
                        TextButton(onClick = { /* Not fully implemented yet */ }) {
                            Text("Add location", color = Color.Gray)
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tag people", color = Color.White)
                        TextButton(onClick = { /* Not fully implemented yet */ }) {
                            Text("Tag", color = Color.Gray)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Privacy", color = Color.White)
                        TextButton(onClick = { privacy = if (privacy == "Public") "Private" else "Public" }) {
                            Text(privacy, color = AccentRed)
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allow Comments", color = Color.White)
                        Switch(checked = allowComments, onCheckedChange = { allowComments = it }, 
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentRed, checkedTrackColor = AccentRed.copy(alpha=0.5f)))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allow Downloads", color = Color.White)
                        Switch(checked = allowDownloads, onCheckedChange = { allowDownloads = it }, 
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentRed, checkedTrackColor = AccentRed.copy(alpha=0.5f)))
                    }

                    Spacer(Modifier.height(32.dp))

                    if (uploadState is UploadState.Error) {
                        Text(uploadState.message, color = Color.Red, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                    }
                    
                    if (uploadState is UploadState.Uploading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AccentRed, trackColor = DarkGray)
                        Spacer(Modifier.height(16.dp))
                    }

                    Button(
                        onClick = {
                            selectedVideoUri?.let { uri ->
                                onUploadVideo(context.applicationContext, uri, caption, privacy, allowComments, allowDownloads)
                            }
                        },
                        enabled = uploadState != UploadState.Uploading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Text(if (uploadState == UploadState.Uploading) "Posting..." else "Post", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewScreen(
    onVideoRecorded: (Uri) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    
    var showFilters by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(FilterSystem.presets.first()) }
    var filterIntensity by remember { mutableStateOf(1f) }

    var showEffects by remember { mutableStateOf(false) }
    var selectedEffect by remember { mutableStateOf(EffectsSystem.presets.first()) }
    
    val colorMatrixState = remember(selectedFilter, filterIntensity) {
        ColorMatrix(FilterSystem.getIntensityMatrix(selectedFilter.matrix, filterIntensity))
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onVideoRecorded(uri)
        }
    }

    val previewView = remember { PreviewView(context) }
    val videoCapture = remember {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        VideoCapture.withOutput(recorder)
    }

    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
                camera?.cameraControl?.enableTorch(isFlashOn)
            } catch (exc: Exception) {
                Log.e("CameraPreview", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(isFlashOn) {
        camera?.cameraControl?.enableTorch(isFlashOn)
    }

    val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
    
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(50, easing = androidx.compose.animation.core.FastOutLinearInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )
    val glitchOffset by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(20, easing = androidx.compose.animation.core.FastOutLinearInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = selectedEffect.scale
                scaleY = selectedEffect.scale
                rotationZ = selectedEffect.rotation
                translationX = selectedEffect.translationX + if (selectedEffect.isGlitch) glitchOffset else if (selectedEffect.isShake) shakeOffset else 0f
                translationY = selectedEffect.translationY + if (selectedEffect.isShake) shakeOffset else 0f
                alpha = selectedEffect.alpha
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val floatArray = FilterSystem.getIntensityMatrix(selectedFilter.matrix, filterIntensity)
                    val colorFilter = android.graphics.ColorMatrixColorFilter(floatArray)
                    val colorEffect = android.graphics.RenderEffect.createColorFilterEffect(colorFilter)
                    
                    if (selectedEffect.blur > 0f) {
                        val blurEffect = android.graphics.RenderEffect.createBlurEffect(selectedEffect.blur, selectedEffect.blur, android.graphics.Shader.TileMode.MIRROR)
                        renderEffect = android.graphics.RenderEffect.createChainEffect(colorEffect, blurEffect).asComposeRenderEffect()
                    } else {
                        renderEffect = colorEffect.asComposeRenderEffect()
                    }
                }
            }
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = "Sound", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Sound", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(24.dp)).padding(vertical = 8.dp)
            ) {
                IconButton(onClick = {
                    lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                        CameraSelector.LENS_FACING_BACK
                    } else {
                        CameraSelector.LENS_FACING_FRONT
                    }
                }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip", tint = Color.White)
                        Text("Flip", color = Color.White, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                IconButton(onClick = { /* Speed */ }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White)
                        Text("Speed", color = Color.White, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                IconButton(onClick = { /* Beauty */ }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Face, contentDescription = "Beauty", tint = Color.White)
                        Text("Beauty", color = Color.White, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                IconButton(onClick = { showFilters = !showFilters }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FilterVintage, contentDescription = "Filter", tint = Color.White)
                        Text("Filter", color = Color.White, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                IconButton(onClick = { /* Timer */ }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Timer, contentDescription = "Timer", tint = Color.White)
                        Text("Timer", color = Color.White, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                IconButton(onClick = { isFlashOn = !isFlashOn }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff, 
                            contentDescription = "Flash", 
                            tint = if (isFlashOn) Color.Yellow else Color.White
                        )
                        Text("Flash", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Options row (Photo/Video/Time)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Camera", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(16.dp))
                Text("Story", color = Color.Gray, fontWeight = FontWeight.Normal)
                Spacer(Modifier.width(16.dp))
                Text("Templates", color = Color.Gray, fontWeight = FontWeight.Normal)
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { showEffects = true }, modifier = Modifier.background(Color.Black.copy(0.3f), CircleShape)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Effects", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Text("Effects", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top=4.dp))
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) Color.DarkGray else AccentRed)
                        .clickable {
                        if (isRecording) {
                            recording?.stop()
                            recording = null
                            isRecording = false
                        } else {
                            val videoFile = File(context.cacheDir, "my_video_${System.currentTimeMillis()}.mp4")
                            val outputOptions = FileOutputOptions.Builder(videoFile).build()
                            
                            val pendingRecording = videoCapture.output
                                .prepareRecording(context, outputOptions)
                            
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                pendingRecording.withAudioEnabled()
                            }
                            
                            recording = pendingRecording.start(mainExecutor) { recordEvent ->
                                if (recordEvent is VideoRecordEvent.Finalize) {
                                    if (!recordEvent.hasError()) {
                                        onVideoRecorded(Uri.fromFile(videoFile))
                                    } else {
                                        recording?.close()
                                        recording = null
                                        isRecording = false
                                        Log.e("Camera", "Video capture failed")
                                        val errorVideoFile = recordEvent.outputResults.outputUri
                                        if(errorVideoFile != Uri.EMPTY) {
                                            File(errorVideoFile.path!!).delete()
                                        }
                                    }
                                }
                            }
                            isRecording = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    Box(modifier = Modifier.size(24.dp).background(AccentRed, RoundedCornerShape(4.dp)))
                } else {
                    Box(modifier = Modifier.size(64.dp).background(Color.White, CircleShape))
                }
            }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { galleryLauncher.launch("video/*") }, modifier = Modifier.background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(8.dp))) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Upload", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Text("Upload", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top=4.dp))
                }
            }
        }
        
        if (showFilters) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showFilters = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {} // Block clicks
                ) {
                    if (selectedFilter.id != "f_0") {
                        Slider(
                            value = filterIntensity,
                            onValueChange = { filterIntensity = it },
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = AccentRed,
                                inactiveTrackColor = Color.Gray
                            )
                        )
                    } else {
                        Spacer(Modifier.height(48.dp))
                    }
                    
                    val categories = FilterSystem.categories
                    var selectedCategory by remember { mutableStateOf<String?>(null) }
                    
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "All",
                                color = if (selectedCategory == null) Color.White else Color.Gray,
                                fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.clickable { selectedCategory = null }
                            )
                        }
                        items(categories.size) { index ->
                            val cat = categories[index]
                            Text(
                                text = cat,
                                color = if (selectedCategory == cat) Color.White else Color.Gray,
                                fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.clickable { selectedCategory = cat }
                            )
                        }
                    }
                    
                    val filteredList = remember(selectedCategory) {
                        if (selectedCategory == null) FilterSystem.presets else FilterSystem.presets.filter { it.category == selectedCategory || it.id == "f_0" }
                    }
                    
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredList.size) { index ->
                            val filter = filteredList[index]
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedFilter = filter }) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(if (selectedFilter == filter) AccentRed else Color.Transparent)
                                        .padding(2.dp)
                                ) {
                                    AsyncImage(
                                        model = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&h=200&fit=crop",
                                        contentDescription = "Filter",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                            androidx.compose.ui.graphics.ColorMatrix(FilterSystem.getIntensityMatrix(filter.matrix, 1f))
                                        )
                                    )
                                }
                                Text(filter.name, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top=4.dp))
                            }
                        }
                    }
                }
            }
        }
        
        if (showEffects) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showEffects = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {} // Block clicks
                ) {
                    Spacer(Modifier.height(16.dp))
                    
                    val categories = EffectsSystem.categories
                    var selectedCategory by remember { mutableStateOf<String?>(categories.first()) }
                    
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(categories.size) { index ->
                            val cat = categories[index]
                            Text(
                                text = cat,
                                color = if (selectedCategory == cat) Color.White else Color.Gray,
                                fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.clickable { selectedCategory = cat }
                            )
                        }
                    }
                    
                    val filteredList = remember(selectedCategory) {
                        if (selectedCategory == null) EffectsSystem.presets else EffectsSystem.presets.filter { it.category == selectedCategory || it.id == "e_0" }
                    }
                    
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredList.size) { index ->
                            val effect = filteredList[index]
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedEffect = effect }) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedEffect == effect) AccentRed else Color.Transparent)
                                        .padding(2.dp)
                                ) {
                                    AsyncImage(
                                        model = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&h=200&fit=crop",
                                        contentDescription = "Effect",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .graphicsLayer {
                                                scaleX = effect.scale
                                                scaleY = effect.scale
                                                rotationZ = effect.rotation
                                                alpha = effect.alpha
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && effect.blur > 0f) {
                                                    renderEffect = android.graphics.RenderEffect.createBlurEffect(effect.blur, effect.blur, android.graphics.Shader.TileMode.MIRROR).asComposeRenderEffect()
                                                }
                                            }
                                    )
                                }
                                Text(effect.name, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top=4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

