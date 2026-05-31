package com.vitaai.app.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.R
import com.vitaai.app.ui.theme.DeepBlue
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.ui.theme.NavyBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onGoLogin: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedQuestion by remember { mutableStateOf("") }
    var securityAnswer by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val securityQuestions = listOf(
        "¿Cuál es el nombre de tu primera mascota?",
        "¿En qué ciudad naciste?",
        "¿Cuál es el apellido de tu madre?",
        "¿Cuál fue el nombre de tu primera escuela?",
        "¿Cuál es tu comida favorita?"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(
                colors = listOf(DeepBlue, NavyBlue, Color(0xFF1E5F8E))
            ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
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
            Text("Crear cuenta", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Gold)
            Text("Empieza tu camino saludable", fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)

            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Tus datos", fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = DeepBlue)
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = username, onValueChange = { username = it },
                        label = { Text("Nombre de usuario") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyBlue, focusedLabelColor = NavyBlue)
                    )
                    Spacer(Modifier.height(12.dp))
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

                    Spacer(Modifier.height(20.dp))
                    Text("🔒 Pregunta de seguridad", fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = DeepBlue)
                    Text("Para recuperar tu contraseña si la olvidas",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedQuestion,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Selecciona una pregunta") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyBlue, focusedLabelColor = NavyBlue)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            securityQuestions.forEach { question ->
                                DropdownMenuItem(
                                    text = { Text(question, fontSize = 13.sp) },
                                    onClick = {
                                        selectedQuestion = question
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = securityAnswer, onValueChange = { securityAnswer = it },
                        label = { Text("Tu respuesta") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyBlue, focusedLabelColor = NavyBlue)
                    )

                    if (errorMsg.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (selectedQuestion.isEmpty()) {
                                errorMsg = "Selecciona una pregunta de seguridad"
                                return@Button
                            }
                            if (securityAnswer.isEmpty()) {
                                errorMsg = "Escribe tu respuesta de seguridad"
                                return@Button
                            }
                            isLoading = true
                            errorMsg = ""
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid ?: return@addOnSuccessListener
                                    db.collection("users").document(uid)
                                        .set(mapOf(
                                            "username" to username,
                                            "email" to email,
                                            "securityQuestion" to selectedQuestion,
                                            "securityAnswer" to securityAnswer.lowercase().trim()
                                        ))
                                        .addOnSuccessListener { onRegisterSuccess() }
                                }
                                .addOnFailureListener {
                                    errorMsg = "Error: ${it.message}"
                                    isLoading = false
                                }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading && username.isNotEmpty()
                                && email.isNotEmpty() && password.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                    ) {
                        if (isLoading) CircularProgressIndicator(
                            modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp
                        )
                        else Text("Registrarse", fontSize = 16.sp, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onGoLogin) {
                Text("¿Ya tienes cuenta? Inicia sesión", color = Gold)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}