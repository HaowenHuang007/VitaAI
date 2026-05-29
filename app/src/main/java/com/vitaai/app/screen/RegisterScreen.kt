package com.vitaai.app.screen

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
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

private const val COLLECTION_USERS = "users"
private const val FIELD_USERNAME = "username"
private const val FIELD_EMAIL = "email"
private const val FIELD_SECURITY_QUESTION = "securityQuestion"
private const val FIELD_SECURITY_ANSWER = "securityAnswer"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onGoLogin: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedQuestion by remember { mutableStateOf("") }
    var securityAnswer by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val securityQuestions = stringArrayResource(R.array.security_questions)

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
                    .background(Gold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) { 
                // TODO: Emoji string, needs to be handled
                Text("🌱", fontSize = 48.sp) 
            }

            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.register_title), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Gold)
            Text(stringResource(R.string.register_tagline), fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)

            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(stringResource(R.string.register_data_title), fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = DeepBlue)
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = username, onValueChange = { username = it },
                        label = { Text(stringResource(R.string.register_username_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyBlue, focusedLabelColor = NavyBlue)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text(stringResource(R.string.common_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyBlue, focusedLabelColor = NavyBlue)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text(stringResource(R.string.common_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyBlue, focusedLabelColor = NavyBlue)
                    )

                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.register_security_question_section), fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = DeepBlue)
                    Text(stringResource(R.string.register_security_question_desc),
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
                            label = { Text(stringResource(R.string.register_select_question_label)) },
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
                        label = { Text(stringResource(R.string.login_security_answer_label)) },
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
                                errorMsg = context.getString(R.string.register_error_select_question)
                                return@Button
                            }
                            if (securityAnswer.isEmpty()) {
                                errorMsg = context.getString(R.string.register_error_empty_answer)
                                return@Button
                            }
                            isLoading = true
                            errorMsg = ""
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid
                                    if (uid == null) {
                                        errorMsg = context.getString(R.string.register_error_uid_null)
                                        isLoading = false
                                        return@addOnSuccessListener
                                    }

                                    db.collection(COLLECTION_USERS).document(uid)
                                        .set(mapOf(
                                            FIELD_USERNAME to username,
                                            FIELD_EMAIL to email,
                                            FIELD_SECURITY_QUESTION to selectedQuestion,
                                            FIELD_SECURITY_ANSWER to securityAnswer.lowercase().trim()
                                        ))
                                        .addOnSuccessListener {
                                            isLoading = false
                                            onRegisterSuccess()
                                        }
                                        .addOnFailureListener {
                                            errorMsg = context.getString(R.string.register_error_save_profile, it.message ?: "")
                                            isLoading = false
                                        }
                                }
                                .addOnFailureListener {
                                    errorMsg = context.getString(R.string.register_error_create_account, it.message ?: "")
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
                        else Text(stringResource(R.string.register_button), fontSize = 16.sp, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onGoLogin) {
                Text(stringResource(R.string.register_go_to_login), color = Gold)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
