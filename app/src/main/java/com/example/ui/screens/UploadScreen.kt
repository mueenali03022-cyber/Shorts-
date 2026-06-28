package com.example.ui.screens

import android.os.Build
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

import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageAnalysis
import com.example.utils.FaceLandmarkerHelper
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer

enum class UploadStep {
    Camera, Preview, Details
}

enum class AREffect {
    NONE, GLASSES, HAT, ANIMAL_EARS, MAKEUP, STICKER,
    NEON_CONTOUR, CYBORG_EYE, FACE_PAINT
}

enum class AnchorType {
    EYES, FOREHEAD, FACE_CENTER, NOSE
}

data class ARAssetConfig(
    val effectType: AREffect,
    val drawableResId: Int?,
    val anchorType: AnchorType,
    val scaleMultiplier: Float,
    val offsetYMultiplier: Float = 0f
)

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
    
    val galleryLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onVideoRecorded(uri)
        }
    }

    val arAssetConfigs = listOf(
        ARAssetConfig(AREffect.GLASSES, com.example.R.drawable.glasses_3d, AnchorType.EYES, 2.5f),
        ARAssetConfig(AREffect.ANIMAL_EARS, com.example.R.drawable.mask_animal, AnchorType.FOREHEAD, 1.5f, -0.8f),
        ARAssetConfig(AREffect.STICKER, com.example.R.drawable.sticker_heart, AnchorType.FACE_CENTER, 1.0f)
    )

    val effectImages = remember { mutableMapOf<AREffect, androidx.compose.ui.graphics.ImageBitmap>() }
    
    // Load images
    arAssetConfigs.forEach { config ->
        if (config.drawableResId != null) {
            val bitmap = androidx.compose.ui.graphics.ImageBitmap.imageResource(id = config.drawableResId)
            LaunchedEffect(Unit) {
                effectImages[config.effectType] = bitmap
            }
        }
    }

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val scannerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )

    val previewView = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    var landmarkerResult by remember { mutableStateOf<FaceLandmarkerHelper.ResultBundle?>(null) }
    var selectedEffect by remember { mutableStateOf(AREffect.NONE) }
    
    val faceLandmarkerHelper = remember {
        FaceLandmarkerHelper(context, object : FaceLandmarkerHelper.LandmarkerListener {
            override fun onError(error: String) {
                Log.e("CameraPreview", "Landmarker error: $error")
            }
            override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
                landmarkerResult = resultBundle
            }
        })
    }
    
    DisposableEffect(Unit) {
        onDispose {
            faceLandmarkerHelper.clearFaceLandmarker()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Failed to unbind camera on dispose", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    val imageAnalyzer = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    faceLandmarkerHelper.detectLiveStream(
                        imageProxy = imageProxy,
                        isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
                    )
                }
            }
    }
    
    val videoCapture = remember {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.FHD))
            .build()
        VideoCapture.withOutput(recorder)
    }

    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            if (lifecycleOwner.lifecycle.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED) return@addListener
            try {
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                cameraProvider.unbindAll()
                try {
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer,
                        videoCapture
                    )
                } catch (e: Exception) {
                    Log.w("CameraPreview", "Failed to bind with selected lens, trying fallback", e)
                    val fallbackLens = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    val fallbackSelector = CameraSelector.Builder().requireLensFacing(fallbackLens).build()
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        fallbackSelector,
                        preview,
                        imageAnalyzer,
                        videoCapture
                    )
                }
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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val result = landmarkerResult ?: return@Canvas
            val faceLandmarksList = result.result.faceLandmarks() ?: return@Canvas
            if (faceLandmarksList.isEmpty()) return@Canvas

            val imageWidth = result.inputImageWidth.toFloat()
            val imageHeight = result.inputImageHeight.toFloat()
            val scaleFactor = maxOf(size.width / imageWidth, size.height / imageHeight)
            
            // To properly map, calculate scaled dimensions
            val scaledWidth = imageWidth * scaleFactor
            val scaledHeight = imageHeight * scaleFactor
            
            val offsetX = (size.width - scaledWidth) / 2f
            val offsetY = (size.height - scaledHeight) / 2f

            for (landmarks in faceLandmarksList) {
                if (selectedEffect == AREffect.NONE) continue
                if (landmarks.size <= 454) continue // Ensure we have enough landmarks
                
                try {
                    // Common indices
                    // Left Eye: 159 (top), 145 (bottom), 33 (left), 133 (right)
                    // Right Eye: 386 (top), 374 (bottom), 362 (right), 263 (left)
                    // Nose Tip: 1
                    // Mouth: 13 (top lip), 14 (bottom lip), 78 (left), 308 (right)
                    // Chin: 152
                    // Forehead: 10
                    // Left Ear: 234
                    // Right Ear: 454
                    
                    val toPixel = { lm: com.google.mediapipe.tasks.components.containers.NormalizedLandmark ->
                        Offset(lm.x() * scaledWidth + offsetX, lm.y() * scaledHeight + offsetY)
                    }

                    // Calculate common anchors
                    val leftEye = toPixel(landmarks[159])
                    val rightEye = toPixel(landmarks[386])
                    val nose = toPixel(landmarks[1])
                    val forehead = toPixel(landmarks[10])

                    val eyeDistance = (rightEye - leftEye).getDistance()
                    val angleRad = kotlin.math.atan2(rightEye.y - leftEye.y, rightEye.x - leftEye.x)
                    val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

                    // Scalable AR Asset Rendering
                    val config = arAssetConfigs.find { it.effectType == selectedEffect }
                    if (config != null && config.drawableResId != null) {
                        val bitmap = effectImages[config.effectType]
                        if (bitmap != null) {
                            val anchorPoint = when (config.anchorType) {
                                AnchorType.EYES -> Offset((leftEye.x + rightEye.x) / 2, (leftEye.y + rightEye.y) / 2)
                                AnchorType.FOREHEAD -> forehead
                                AnchorType.FACE_CENTER, AnchorType.NOSE -> nose
                            }

                            val baseWidth = when (config.anchorType) {
                                AnchorType.EYES, AnchorType.FOREHEAD -> eyeDistance
                                else -> eyeDistance * 2f
                            }
                            
                            val targetWidth = baseWidth * config.scaleMultiplier
                            val ratio = bitmap.height.toFloat() / bitmap.width.toFloat()
                            val targetHeight = targetWidth * ratio
                            
                            val finalCenter = Offset(
                                anchorPoint.x,
                                anchorPoint.y + (targetHeight * config.offsetYMultiplier)
                            )

                            withTransform({
                                translate(left = finalCenter.x, top = finalCenter.y)
                                rotate(degrees = angleDeg)
                            }) {
                                drawImage(
                                    image = bitmap,
                                    dstOffset = androidx.compose.ui.unit.IntOffset(-targetWidth.toInt() / 2, -targetHeight.toInt() / 2),
                                    dstSize = androidx.compose.ui.unit.IntSize(targetWidth.toInt(), targetHeight.toInt())
                                )
                            }
                        }
                    } else if (selectedEffect == AREffect.CYBORG_EYE) {
                        val radius = eyeDistance * 0.8f
                        withTransform({
                            translate(left = rightEye.x, top = rightEye.y)
                            rotate(degrees = angleDeg)
                        }) {
                            // Glowing red eye
                            drawCircle(color = Color.Red.copy(alpha=0.3f), radius = radius * 1.5f)
                            drawCircle(color = Color.Red.copy(alpha=0.6f), radius = radius)
                            drawCircle(color = Color.White, radius = radius * 0.2f)
                            
                            // Scanner line using infinite transition
                            val scannerY = scannerOffset * radius
                            drawLine(
                                color = Color.Red,
                                start = Offset(-radius * 1.2f, scannerY),
                                end = Offset(radius * 1.2f, scannerY),
                                strokeWidth = 4f
                            )
                        }
                    } else if (selectedEffect == AREffect.NEON_CONTOUR) {
                        // Face Oval
                        val ovalIndices = listOf(10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288, 397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136, 172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109)
                        val ovalPath = androidx.compose.ui.graphics.Path().apply {
                            ovalIndices.forEachIndexed { index, i ->
                                val pt = toPixel(landmarks[i])
                                if (index == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                            }
                            close()
                        }
                        drawPath(ovalPath, color = Color.Cyan.copy(alpha = 0.8f), style = Stroke(width = 5f))
                        drawPath(ovalPath, color = Color.Blue.copy(alpha = 0.4f), style = Stroke(width = 15f)) // glow
                    } else if (selectedEffect == AREffect.FACE_PAINT) {
                        // Face paint strips
                        val fhLeft = toPixel(landmarks[71])
                        val fhRight = toPixel(landmarks[301])
                        drawLine(color = Color.Yellow.copy(alpha=0.7f), start = fhLeft, end = fhRight, strokeWidth = 15f)
                        
                        val cheekLeftStart = toPixel(landmarks[116])
                        val cheekLeftEnd = toPixel(landmarks[205])
                        drawLine(color = Color.Red.copy(alpha=0.7f), start = cheekLeftStart, end = cheekLeftEnd, strokeWidth = 10f)
                        
                        val cheekRightStart = toPixel(landmarks[345])
                        val cheekRightEnd = toPixel(landmarks[425])
                        drawLine(color = Color.Red.copy(alpha=0.7f), start = cheekRightStart, end = cheekRightEnd, strokeWidth = 10f)
                    } else if (selectedEffect == AREffect.MAKEUP) {
                        val leftLip = toPixel(landmarks[78])
                        val rightLip = toPixel(landmarks[308])
                        val topLip = toPixel(landmarks[13])
                        val bottomLip = toPixel(landmarks[14])
                        
                        val lipPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(leftLip.x, leftLip.y)
                            quadraticTo(topLip.x, topLip.y - 10f, rightLip.x, rightLip.y)
                            quadraticTo(bottomLip.x, bottomLip.y + 10f, leftLip.x, leftLip.y)
                            close()
                        }
                        drawPath(lipPath, color = Color.Red.copy(alpha=0.4f))
                        
                        val leftCheek = toPixel(landmarks[116])
                        val rightCheek = toPixel(landmarks[345])
                        drawCircle(color = Color.Magenta.copy(alpha=0.2f), radius = eyeDistance * 0.5f, center = leftCheek)
                        drawCircle(color = Color.Magenta.copy(alpha=0.2f), radius = eyeDistance * 0.5f, center = rightCheek)
                    }
                } catch (e: Exception) {
                    // Ignore drawing errors if landmarks are incomplete or invalid
                }
            }
        }
        
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
            // AR Effects Row
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(AREffect.values().size) { index ->
                    val effect = AREffect.values()[index]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedEffect = effect }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (selectedEffect == effect) AccentRed else Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (effect) {
                                    AREffect.NONE -> "🚫"
                                    AREffect.GLASSES -> "👓"
                                    AREffect.HAT -> "🎩"
                                    AREffect.STICKER -> "❤️"
                                    AREffect.ANIMAL_EARS -> "🐱"
                                    AREffect.MAKEUP -> "💄"
                                    AREffect.NEON_CONTOUR -> "✨"
                                    AREffect.CYBORG_EYE -> "🤖"
                                    AREffect.FACE_PAINT -> "🎨"
                                },
                                fontSize = 24.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = effect.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                            color = if (selectedEffect == effect) AccentRed else Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
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
                Spacer(modifier = Modifier.width(64.dp))

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
    }
}

