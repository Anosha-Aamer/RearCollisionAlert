package com.example.rearcollision

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.rearcollision.ui.theme.RearCollisionAlertTheme
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var tts: TextToSpeech
    private var lastAlertTime = 0L

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Log.e("Camera", "Camera permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            RearCollisionAlertTheme {
                RearCollisionApp(
                    onSpeakAlert = { message -> speakAlert(message) },
                    cameraExecutor = cameraExecutor
                )
            }
        }
    }

    private fun speakAlert(message: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAlertTime > 3000) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
            lastAlertTime = currentTime
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        tts.shutdown()
    }
}

@Composable
fun RearCollisionApp(
    onSpeakAlert: (String) -> Unit,
    cameraExecutor: ExecutorService
) {
    var distanceStatus by remember { mutableStateOf("SAFE") }
    var distanceValue by remember { mutableStateOf(0f) }
    var objectDetected by remember { mutableStateOf(false) }
    var frameCount by remember { mutableStateOf(0) }

    val statusColor = when (distanceStatus) {
        "DANGER" -> Color(0xFFFF3333)
        "WARNING" -> Color(0xFFFFAA00)
        else -> Color(0xFF00CC44)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Camera Preview
        AndroidView(
            factory = { context ->
                val previewView = PreviewView(context)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                frameCount++
                                val width = imageProxy.width
                                val height = imageProxy.height
                                val centerX = width / 2
                                val centerY = height / 2

                                // Simulate distance using image brightness
                                val buffer = imageProxy.planes[0].buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)

                                var brightnessSum = 0L
                                val sampleSize = minOf(bytes.size, 1000)
                                for (i in 0 until sampleSize) {
                                    brightnessSum += (bytes[i].toInt() and 0xFF)
                                }
                                val avgBrightness = brightnessSum / sampleSize.toFloat()

                                // Distance estimation based on brightness change
                                val estimatedDistance = (avgBrightness / 255f) * 10f

                                distanceValue = estimatedDistance
                                objectDetected = estimatedDistance < 8f

                                distanceStatus = when {
                                    estimatedDistance < 2f -> {
                                        onSpeakAlert("Danger! Object very close!")
                                        "DANGER"
                                    }
                                    estimatedDistance < 5f -> {
                                        onSpeakAlert("Warning! Object nearby!")
                                        "WARNING"
                                    }
                                    else -> "SAFE"
                                }

                                imageProxy.close()
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            context as androidx.lifecycle.LifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        Log.e("Camera", "Binding failed", e)
                    }

                }, ContextCompat.getMainExecutor(context))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Status Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚗 REAR COLLISION ALERT",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Frame: $frameCount",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Bottom Dashboard
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(statusColor, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (distanceStatus) {
                                "DANGER" -> "🔴 DANGER - STOP!"
                                "WARNING" -> "⚠️ WARNING - SLOW DOWN"
                                else -> "✅ SAFE - All Clear"
                            },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Distance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Distance", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                text = String.format("%.1f m", distanceValue),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Object", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                text = if (objectDetected) "DETECTED" else "CLEAR",
                                color = if (objectDetected) Color.Red else Color.Green,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Zone", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                text = distanceStatus,
                                color = statusColor,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Zone indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ZoneBox("SAFE", Color(0xFF00CC44), distanceStatus == "SAFE")
                        ZoneBox("WARNING", Color(0xFFFFAA00), distanceStatus == "WARNING")
                        ZoneBox("DANGER", Color(0xFFFF3333), distanceStatus == "DANGER")
                    }
                }
            }
        }
    }
}

@Composable
fun ZoneBox(label: String, color: Color, isActive: Boolean) {
    Box(
        modifier = Modifier
            .width(90.dp)
            .background(
                if (isActive) color else color.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}