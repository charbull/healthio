package com.healthio.ui.vision

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
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
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasPermission by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

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
            if (hasPermission) {
                if (state is VisionState.Idle || state is VisionState.Analyzing) {
                    CameraContent(
                        onImageCaptured = { bitmap -> viewModel.analyzeImage(bitmap) },
                        isAnalyzing = state is VisionState.Analyzing
                    )
                } else if (state is VisionState.Success) {
                    val result = (state as VisionState.Success).analysis
                    ResultContent(result)
                } else if (state is VisionState.Error) {
                    val error = (state as VisionState.Error).message
                    ErrorContent(error, onRetry = { viewModel.reset() })
                }
            } else {
                Text(
                    "Camera permission required.",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun CameraContent(onImageCaptured: (Bitmap) -> Unit, isAnalyzing: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
        
        if (isAnalyzing) {
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
        } else {
            Button(
                onClick = {
                    takePhoto(context, imageCapture, onImageCaptured)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
                    .height(64.dp)
            ) {
                Text("SNAP & ANALYZE")
            }
        }
    }
}

@Composable
fun ResultContent(analysis: com.healthio.core.ai.FoodAnalysis) {
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
        
        // Score Badge
        val scoreColor = when {
            analysis.healthScore >= 8 -> Color(0xFF4CAF50) // Green
            analysis.healthScore >= 5 -> Color(0xFFFF9800) // Orange
            else -> Color(0xFFF44336) // Red
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Macros
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MacroItem("Calories", analysis.calories.toString())
            MacroItem("Protein", "${analysis.protein}g")
            MacroItem("Carbs", "${analysis.carbs}g")
            MacroItem("Fat", "${analysis.fat}g")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = analysis.feedback,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun MacroItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
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
                // Handle error
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // Load bitmap and resize
                val bitmap = BitmapFactory.decodeFile(File(context.cacheDir, "temp.jpg").absolutePath)
                // Resize to max 1024 to save bandwidth
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

private suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener({
        continuation.resume(cameraProviderFuture.get())
    }, ContextCompat.getMainExecutor(this))
}
