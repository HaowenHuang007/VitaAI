package com.vitaai.app.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.vitaai.app.utils.XPManager

@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    var showCamera by remember { mutableStateOf(true) }
    var savedMsg by remember { mutableStateOf("") }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    Column(modifier = modifier.fillMaxSize()) {
        if (showCamera) {
            // Vista de cámara
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val imageCaptureBuilder = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = imageCaptureBuilder

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCaptureBuilder
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay con guía
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                    Text(
                        "📸 Apunta al alimento",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp),
                        fontSize = 16.sp
                    )
                }

                // Botón de captura
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            isAnalyzing = true
                            val capture = imageCapture ?: return@Button
                            capture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bitmap = imageProxyToBitmap(image)
                                        image.close()
                                        scope.launch {
                                            try {
                                                val base64 = bitmapToBase64(bitmap)
                                                result = analyzeImageWithOpenAI(base64)
                                                showCamera = false
                                            } catch (e: Exception) {
                                                result = "Error al analizar: ${e.message}"
                                            }
                                            isAnalyzing = false
                                        }
                                    }
                                    override fun onError(exception: ImageCaptureException) {
                                        isAnalyzing = false
                                    }
                                }
                            )
                        },
                        modifier = Modifier.size(72.dp).clip(CircleShape),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        enabled = !isAnalyzing
                    ) {
                        if (isAnalyzing) CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        else Text("📸", fontSize = 24.sp)
                    }
                }
            }
        } else {
            // Resultado
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Text("🔍 Análisis nutricional", fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🤖 Resultado de IA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(result, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (savedMsg.isNotEmpty()) {
                    Text(savedMsg, color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        db.collection("users").document(uid)
                            .collection("foodlog")
                            .add(mapOf(
                                "date" to today,
                                "analysis" to result,
                                "timestamp" to System.currentTimeMillis()
                            ))
                            .addOnSuccessListener {
                                XPManager.addXPWithLimit(
                                    amount = XPManager.XP_FOOD_SCAN,
                                    actionKey = XPManager.KEY_FOOD,
                                    dailyLimit = 3
                                ) { added ->
                                    savedMsg = if (added > 0)
                                        "✅ Guardado · +${added} XP 🏆"
                                    else
                                        "✅ Guardado (máximo 3 fotos con XP por día)"
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("💾 Guardar en mi registro", fontSize = 16.sp)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        showCamera = true
                        result = ""
                        savedMsg = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📸 Tomar otra foto", fontSize = 16.sp)
                }
            }
        }
    }
}

fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

fun bitmapToBase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

suspend fun analyzeImageWithOpenAI(base64Image: String): String = withContext(Dispatchers.IO) {
    val apiKey = "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"
    val url = URL("https://api.openai.com/v1/chat/completions")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("Authorization", "Bearer $apiKey")
    connection.doOutput = true

    val imageContent = JSONObject().apply {
        put("type", "image_url")
        put("image_url", JSONObject().apply {
            put("url", "data:image/jpeg;base64,$base64Image")
        })
    }

    val textContent = JSONObject().apply {
        put("type", "text")
        put("text", """
            Analiza este alimento y responde en español con:
            🍽️ Alimento identificado: [nombre]
            🔥 Calorías estimadas: [número] kcal
            💪 Proteínas: [g]
            🍞 Carbohidratos: [g]
            🥑 Grasas: [g]
            ✅ Valoración: [saludable/moderado/evitar]
            💡 Consejo: [1 consejo nutricional breve]
        """.trimIndent())
    }

    val body = JSONObject().apply {
        put("model", "gpt-4o-mini")
        put("messages", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", JSONArray().apply {
                    put(textContent)
                    put(imageContent)
                })
            })
        })
        put("max_tokens", 500)
    }.toString()

    connection.outputStream.write(body.toByteArray())
    val response = connection.inputStream.bufferedReader().readText()
    JSONObject(response)
        .getJSONArray("choices")
        .getJSONObject(0)
        .getJSONObject("message")
        .getString("content")
}