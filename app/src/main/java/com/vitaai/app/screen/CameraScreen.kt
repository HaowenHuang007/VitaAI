package com.vitaai.app.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.utils.AchievementManager
import com.vitaai.app.utils.FoodAnalysis
import com.vitaai.app.utils.NutritionLog
import com.vitaai.app.utils.StreakManager
import com.vitaai.app.utils.XPManager
import com.vitaai.app.utils.analyzeImageWithOpenAI
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

private const val USERS_COLLECTION = "users"
private const val FOODLOG_COLLECTION = "foodlog"
private const val DATE_FORMAT = "yyyy-MM-dd"
private const val FIRST_FOOD_ACHIEVEMENT = "first_food"

@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> cameraPermissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    var analysis by remember { mutableStateOf<FoodAnalysis?>(null) }
    var showCamera by remember { mutableStateOf(true) }
    var savedMsg by remember { mutableStateOf("") }
    
    val langName = Locale.getDefault().displayLanguage

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    if (!cameraPermissionGranted) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(
                    imageVector = IconList.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.camera_permission_required),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.camera_grant_permission))
                }
            }
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (showCamera) {
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

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                    Text(
                        text = stringResource(R.string.camera_point_at_food),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp),
                        fontSize = 16.sp
                    )
                }

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
                                        val rotation = image.imageInfo.rotationDegrees
                                        val bitmap = imageProxyToBitmap(image, rotation)
                                        image.close()
                                        scope.launch {
                                            try {
                                                val base64 = bitmapToBase64(bitmap)
                                                val a = analyzeImageWithOpenAI(base64, langName)
                                                analysis = a
                                                result = a.displayText
                                                showCamera = false
                                            } catch (e: Exception) {
                                                result = context.getString(R.string.camera_error_analyzing) + ": ${e.message}"
                                                analysis = null
                                                showCamera = false
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
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        enabled = !isAnalyzing
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = IconList.Camera,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(IconList.Search, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.camera_nutritional_analysis),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(IconList.Nutritionist, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.camera_ai_result), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(result, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (savedMsg.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(IconList.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(savedMsg, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        val today = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
                        val a = analysis
                        db.collection(USERS_COLLECTION).document(uid)
                            .collection(FOODLOG_COLLECTION)
                            .add(mapOf(
                                "date" to today,
                                "analysis" to result,
                                "name" to (a?.name ?: ""),
                                "calories" to (a?.calories ?: 0),
                                "proteinG" to (a?.proteinG ?: 0),
                                "carbsG" to (a?.carbsG ?: 0),
                                "fatG" to (a?.fatG ?: 0),
                                "rating" to (a?.rating ?: ""),
                                "timestamp" to System.currentTimeMillis()
                            ))
                            .addOnSuccessListener {
                                if (a != null) {
                                    NutritionLog.addMeal(a.calories, a.proteinG, a.carbsG, a.fatG)
                                }
                                StreakManager.recordActivity()
                                AchievementManager.unlock(FIRST_FOOD_ACHIEVEMENT)
                                XPManager.addXPWithLimit(
                                    amount = XPManager.XP_FOOD_SCAN,
                                    actionKey = XPManager.KEY_FOOD,
                                    dailyLimit = 3
                                ) { added ->
                                    savedMsg = if (added > 0) {
                                        context.getString(R.string.camera_saved_xp_format, added)
                                    } else {
                                        context.getString(R.string.camera_saved_limit)
                                    }
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(IconList.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.camera_save_to_log), fontSize = 16.sp)
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
                    Icon(IconList.Camera, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.camera_take_another_photo), fontSize = 16.sp)
                }
            }
        }
    }
}

fun imageProxyToBitmap(image: ImageProxy, rotationDegrees: Int = 0): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    if (rotationDegrees == 0) return raw
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
}

fun bitmapToBase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}
