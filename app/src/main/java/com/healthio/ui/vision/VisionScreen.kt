package com.healthio.ui.vision

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionScreen(
    onBack: () -> Unit,
    viewModel: VisionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    var hasPermission by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = uriToBitmap(context, uri)
            val resized = resizeBitmap(bitmap, 1024)
            viewModel.onImageCaptured(resized)
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Vision") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state is VisionState.Success) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "New Scan")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // We allow gallery access even without camera permission if needed, but for simplicity keeping nested
            // Actually, let's allow it even if camera permission denied?
            // But 'hasPermission' guards the whole block.
            // Let's modify logic to allow gallery always.
            
            when (state) {
                is VisionState.Idle -> {
                    CameraContent(
                        hasPermission = hasPermission,
                        onImageCaptured = { bitmap -> viewModel.onImageCaptured(bitmap) },
                        onGalleryClick = { 
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) }
                    )
                }
                is VisionState.Review -> {
                    val bitmap = (state as VisionState.Review).bitmap
                    ReviewContent(
                        bitmap = bitmap,
                        onAnalyze = { contextText -> viewModel.analyzeImage(contextText) },
                        onRetake = { viewModel.reset() }
                    )
                }
                is VisionState.Analyzing -> {
                    LoadingContent()
                }
                                    is VisionState.Success -> {
                                        val result = (state as VisionState.Success).analysis
                                        ResultContent(
                                            analysis = result,
                                            onSave = { viewModel.saveLog(result) }
                                        )
                                    }                is VisionState.Error -> {
                    val error = (state as VisionState.Error).message
                    ErrorContent(error, onRetry = { viewModel.reset() })
                }
            }
        }
    }
}

@Composable
fun ReviewContent(
    bitmap: Bitmap,
    onAnalyze: (String) -> Unit,
    onRetake: () -> Unit
) {
    var contextText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = contextText,
            onValueChange = { contextText = it },
            label = { Text("Add Context (Optional)") },
            placeholder = { Text("e.g. Vegan burger, homemade pasta...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f)
            ) {
                Text("Retake")
            }
            
            Button(
                onClick = { onAnalyze(contextText) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Analyze")
            }
        }
    }
}

@Composable
fun LoadingContent() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Analyzing Food...", color = Color.White)
        }
    }
}

@Composable
fun CameraContent(
    hasPermission: Boolean,
    onImageCaptured: (Bitmap) -> Unit,
    onGalleryClick: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    if (hasPermission) {
        LaunchedEffect(Unit) {
            val cameraProvider = context.getCameraProvider()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                // Log error
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission required.")
                Button(onClick = onRequestPermission) {
                    Text("Grant Permission")
                }
            }
        }
        
        // Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery Button
            FloatingActionButton(
                onClick = onGalleryClick,
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_gallery), // Using system icon
                    contentDescription = "Gallery"
                )
            }
            
            // Snap Button (Only if has permission)
            if (hasPermission) {
                FloatingActionButton(
                    onClick = {
                        takePhoto(context, imageCapture, onImageCaptured)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_camera),
                        contentDescription = "Snap",
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(72.dp)) // Placeholder
            }
            
            // Spacer to balance layout
            Spacer(modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
fun ResultContent(
    analysis: com.healthio.core.ai.FoodAnalysis,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = analysis.foodName,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val scoreColor = when {
            analysis.healthScore >= 8 -> Color(0xFF4CAF50)
            analysis.healthScore >= 5 -> Color(0xFFFF9800)
            else -> Color(0xFFF44336)
        }
        
        Surface(
            color = scoreColor,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "Health Score: ${analysis.healthScore}/10",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Macros with Progress Bars
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MacroBar(label = "Calories", value = analysis.calories, unit = "kcal", max = 1000, color = Color.Gray)
            MacroBar(label = "Protein", value = analysis.protein, unit = "g", max = 50, color = Color(0xFF2196F3))
            MacroBar(label = "Carbs", value = analysis.carbs, unit = "g", max = 100, color = Color(0xFFFFC107))
            MacroBar(label = "Fat", value = analysis.fat, unit = "g", max = 50, color = Color(0xFFE91E63))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f), // Allow scrolling if needed, or flexible height
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = analysis.feedback,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Save Log")
        }
    }
}

@Composable
fun MacroBar(label: String, value: Int, unit: String, max: Int, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = "$value$unit",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = (value.toFloat() / max).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.1f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun ErrorContent(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Error", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        Text(text = error, modifier = Modifier.padding(16.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (Bitmap) -> Unit
) {
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        File(context.cacheDir, "temp.jpg")
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(File(context.cacheDir, "temp.jpg").absolutePath)
                val resized = resizeBitmap(bitmap, 1024)
                onImageCaptured(resized)
            }
        }
    )
}

private fun resizeBitmap(source: Bitmap, maxLength: Int): Bitmap {
    try {
        if (source.height >= source.width) {
            if (source.height <= maxLength) return source
            val aspectRatio = source.width.toDouble() / source.height.toDouble()
            val targetWidth = (maxLength * aspectRatio).toInt()
            return Bitmap.createScaledBitmap(source, targetWidth, maxLength, false)
        } else {
            if (source.width <= maxLength) return source
            val aspectRatio = source.height.toDouble() / source.width.toDouble()
            val targetHeight = (maxLength * aspectRatio).toInt()
            return Bitmap.createScaledBitmap(source, maxLength, targetHeight, false)
        }
    } catch (e: Exception) {
        return source
    }
}

private fun uriToBitmap(context: Context, uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.isMutableRequired = true // Important for resizing sometimes
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener({
        continuation.resume(cameraProviderFuture.get())
    }, ContextCompat.getMainExecutor(this))
}