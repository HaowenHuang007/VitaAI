package com.vitaai.app.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.vitaai.app.ui.theme.DeepBlue
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.ui.theme.NavyBlue

@Composable
fun VerifyScreen(onGoLogin: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    var resendMsg by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(DeepBlue, NavyBlue)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Gold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📧", fontSize = 52.sp)
            }

            Spacer(Modifier.height(24.dp))
            Text("Verifica tu email", fontSize = 28.sp,
                fontWeight = FontWeight.Bold, color = Gold,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                "Hemos enviado un email de verificación a:\n${auth.currentUser?.email ?: ""}",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Revisa tu bandeja de entrada y haz clic en el enlace para activar tu cuenta.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            auth.signOut()
                            onGoLogin()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                    ) {
                        Text("Ya verifiqué, ir a iniciar sesión",
                            fontSize = 15.sp, color = Color.White)
                    }

                    Spacer(Modifier.height(12.dp))

                    if (resendMsg.isNotEmpty()) {
                        Text(resendMsg, fontSize = 13.sp,
                            color = if (resendMsg.startsWith("✅")) NavyBlue
                            else MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                    }

                    OutlinedButton(
                        onClick = {
                            isSending = true
                            auth.currentUser?.sendEmailVerification()
                                ?.addOnSuccessListener {
                                    resendMsg = "✅ Email reenviado"
                                    isSending = false
                                }
                                ?.addOnFailureListener {
                                    resendMsg = "❌ Error al reenviar"
                                    isSending = false
                                }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSending
                    ) {
                        if (isSending) CircularProgressIndicator(
                            modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                        )
                        else Text("Reenviar email de verificación", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}