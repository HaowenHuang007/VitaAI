package com.vitaai.app.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.vitaai.app.icons.IconList
import com.vitaai.app.ui.theme.DeepBlue
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.ui.theme.NavyBlue

private const val COLLECTION_USERS = "users"
private const val FIELD_USERNAME = "username"
private const val FIELD_EMAIL = "email"
private const val GOOGLE_FAVICON_URL = "https://www.google.com/favicon.ico"

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onGoRegister: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scrollState = rememberScrollState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    // State for recovery dialog
    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var forgotMsg by remember { mutableStateOf("") }
    var isForgotLoading by remember { mutableStateOf(false) }
    var isEmailSentSuccess by remember { mutableStateOf(false) }

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
                        val uid = authResult.user?.uid ?: run {
                            isGoogleLoading = false
                            return@addOnSuccessListener
                        }
                        val isNew = authResult.additionalUserInfo?.isNewUser == true
                        if (isNew) {
                            db.collection(COLLECTION_USERS).document(uid)
                                .set(mapOf(
                                    FIELD_USERNAME to (account.displayName ?: ""),
                                    FIELD_EMAIL to (account.email ?: "")
                                ))
                                .addOnSuccessListener {
                                    isGoogleLoading = false
                                    onLoginSuccess()
                                }
                                .addOnFailureListener {
                                    errorMsg = context.getString(R.string.login_error_create_profile, it.message ?: "")
                                    isGoogleLoading = false
                                }
                        } else {
                            isGoogleLoading = false
                            onLoginSuccess()
                        }
                    }
                    .addOnFailureListener {
                        errorMsg = context.getString(R.string.common_error_generic, it.message ?: "")
                        isGoogleLoading = false
                    }
            } catch (e: ApiException) {
                errorMsg = context.getString(R.string.login_error_google_sign_in)
                isGoogleLoading = false
            }
        }
    }

    // Professional Recovery Dialog
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isForgotLoading) {
                    showForgotDialog = false
                    forgotMsg = ""
                    forgotEmail = ""
                    isEmailSentSuccess = false
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = IconList.Lock, contentDescription = null, tint = NavyBlue, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.login_forgot_password_title), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    if (isEmailSentSuccess) {
                        Text(
                            text = stringResource(R.string.login_email_sent_success, forgotEmail),
                            fontSize = 14.sp,
                            color = NavyBlue
                        )
                    } else {
                        Text(stringResource(R.string.login_forgot_password_desc),
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = forgotEmail, onValueChange = { forgotEmail = it },
                            label = { Text(stringResource(R.string.common_email)) },
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isEmailSentSuccess) {
                            showForgotDialog = false
                            isEmailSentSuccess = false
                            forgotEmail = ""
                        } else {
                            isForgotLoading = true
                            forgotMsg = ""
                            // Direct email sending via Firebase
                            auth.sendPasswordResetEmail(forgotEmail)
                                .addOnSuccessListener {
                                    isForgotLoading = false
                                    isEmailSentSuccess = true
                                }
                                .addOnFailureListener {
                                    isForgotLoading = false
                                    forgotMsg = context.getString(R.string.login_error_send_email)
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                    enabled = !isForgotLoading && (forgotEmail.isNotEmpty() || isEmailSentSuccess)
                ) {
                    if (isForgotLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp
                        )
                    } else {
                        if (isEmailSentSuccess) {
                            Text(stringResource(R.string.common_close), color = Color.White)
                        } else {
                            Text(stringResource(R.string.common_send), color = Color.White)
                        }
                    }
                }
            },
            dismissButton = {
                if (!isEmailSentSuccess) {
                    TextButton(onClick = { showForgotDialog = false; forgotMsg = "" }) {
                        Text(stringResource(R.string.common_cancel))
                    }
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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Gold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = IconList.Logo,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = IconList.PlantGreen
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.app_name), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Gold)
            Text(stringResource(R.string.login_tagline), fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)

            Spacer(Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(stringResource(R.string.login_title), fontSize = 20.sp,
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
                                    model = GOOGLE_FAVICON_URL,
                                    contentDescription = stringResource(R.string.common_google),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.login_google_button), fontSize = 15.sp, color = DeepBlue)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.common_or), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            forgotEmail = email
                            forgotMsg = ""
                            showForgotDialog = true
                        }) {
                            Text(stringResource(R.string.login_forgot_password_link), fontSize = 12.sp, color = NavyBlue)
                        }
                    }

                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(8.dp))
                    // Main Login Button
                    Button(
                        onClick = {
                            isLoading = true
                            errorMsg = ""
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener { onLoginSuccess() }
                                .addOnFailureListener {
                                    errorMsg = context.getString(R.string.login_error_invalid_credentials)
                                    isLoading = false
                                }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading && !isGoogleLoading
                                && email.isNotEmpty() && password.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.login_title), fontSize = 16.sp, color = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onGoRegister) {
                Text(stringResource(R.string.login_go_to_register), color = Gold)
            }
        }
    }
}
