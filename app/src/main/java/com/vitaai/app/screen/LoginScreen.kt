package com.vitaai.app.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.R
import com.vitaai.app.ui.theme.DeepBlue
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.ui.theme.NavyBlue

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onGoRegister: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    var showForgotStep1 by remember { mutableStateOf(false) }
    var showForgotStep2 by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var securityQuestion by remember { mutableStateOf("") }
    var securityAnswer by remember { mutableStateOf("") }
    var correctAnswer by remember { mutableStateOf("") }
    var forgotMsg by remember { mutableStateOf("") }
    var isForgotLoading by remember { mutableStateOf(false) }

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                isGoogleLoading = true
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid ?: return@addOnSuccessListener
                        val isNew = authResult.additionalUserInfo?.isNewUser == true
                        if (isNew) {
                            db.collection("users").document(uid)
                                .set(mapOf(
                                    "username" to (account.displayName ?: ""),
                                    "email" to (account.email ?: "")
                                ))
                                .addOnSuccessListener { onLoginSuccess() }
                        } else {
                            onLoginSuccess()
                        }
                        isGoogleLoading = false
                    }
                    .addOnFailureListener {
                        errorMsg = "Error con Google: ${it.message}"
                        isGoogleLoading = false
                    }
            } catch (e: ApiException) {
                errorMsg = "Error de Google Sign In"
                isGoogleLoading = false
            }
        }
    }

    if (showForgotStep1) {
        AlertDialog(
            onDismissRequest = { showForgotStep1 = false; forgotMsg = ""; forgotEmail = "" },
            title = { Text("Recuperar contraseña", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Introduce tu email registrado",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = forgotEmail, onValueChange = { forgotEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyBlue, focusedLabelColor = NavyBlue)
                    )
                    if (forgotMsg.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(forgotMsg, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isForgotLoading = true
                        forgotMsg = ""
                        db.collection("users")
                            .whereEqualTo("email", forgotEmail)
                            .get()
                            .addOnSuccessListener { snap ->
                                if (snap.isEmpty) {
                                    forgotMsg = "❌ No existe cuenta con ese email"
                                    isForgotLoading = false
                                } else {
                                    val doc = snap.documents.first()
                                    securityQuestion = doc.getString("securityQuestion") ?: ""
                                    correctAnswer = doc.getString("securityAnswer") ?: ""
                                    isForgotLoading = false
                                    showForgotStep1 = false
                                    showForgotStep2 = true
                                }
                            }
                            .addOnFailureListener {
                                forgotMsg = "❌ Error al buscar cuenta"
                                isForgotLoading = false
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                    enabled = !isForgotLoading && forgotEmail.isNotEmpty()
                ) {
                    if (isForgotLoading) CircularProgressIndicator(
                        modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp
                    )
                    else Text("Siguiente", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotStep1 = false; forgotMsg = "" }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showForgotStep2) {
        AlertDialog(
            onDismissRequest = { showForgotStep2 = false; securityAnswer = ""; forgotMsg = "" },
            title = { Text("Pregunta de seguridad", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(securityQuestion, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = DeepBlue)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = securityAnswer, onValueChange = { securityAnswer = it },
                        label = { Text("Tu respuesta") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyBlue, focusedLabelColor = NavyBlue)
                    )
                    if (forgotMsg.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(forgotMsg, fontSize = 13.sp,
                            color = if (forgotMsg.startsWith("✅")) NavyBlue
                            else MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (securityAnswer.lowercase().trim() == correctAnswer) {
                            auth.sendPasswordResetEmail(forgotEmail)
                                .addOnSuccessListener { forgotMsg = "✅ Email enviado a $forgotEmail" }
                                .addOnFailureListener { forgotMsg = "❌ Error al enviar email" }
                        } else {
                            forgotMsg = "❌ Respuesta incorrecta"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                    enabled = securityAnswer.isNotEmpty() && !forgotMsg.startsWith("✅")
                ) {
                    Text("Verificar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForgotStep2 = false; securityAnswer = ""; forgotMsg = ""
                }) {
                    Text(if (forgotMsg.startsWith("✅")) "Cerrar" else "Cancelar")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(
                colors = listOf(DeepBlue, NavyBlue, Color(0xFF1E5F8E))
            ))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF010721)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_mark),
                    contentDescription = "VitaAI",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("VitaAI", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Gold)
            Text("Tu asistente de salud inteligente", fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)

            Spacer(Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Iniciar sesión", fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = DeepBlue)
                    Spacer(Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = {
                            isGoogleLoading = true
                            errorMsg = ""
                            launcher.launch(googleSignInClient.signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isGoogleLoading && !isLoading
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                AsyncImage(
                                    model = "https://www.google.com/favicon.ico",
                                    contentDescription = "Google",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Continuar con Google", fontSize = 15.sp, color = DeepBlue)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text("  o  ", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyBlue, focusedLabelColor = NavyBlue)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyBlue, focusedLabelColor = NavyBlue)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            forgotEmail = email
                            forgotMsg = ""
                            showForgotStep1 = true
                        }) {
                            Text("¿Olvidaste tu contraseña?", fontSize = 12.sp, color = NavyBlue)
                        }
                    }

                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            errorMsg = ""
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener { onLoginSuccess() }
                                .addOnFailureListener {
                                    errorMsg = "Email o contraseña incorrectos"
                                    isLoading = false
                                }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading && !isGoogleLoading
                                && email.isNotEmpty() && password.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                    ) {
                        if (isLoading) CircularProgressIndicator(
                            modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp
                        )
                        else Text("Iniciar sesión", fontSize = 16.sp, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onGoRegister) {
                Text("¿No tienes cuenta? Regístrate", color = Gold)
            }
        }
    }
}