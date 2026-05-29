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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.ui.theme.DeepBlue
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.ui.theme.NavyBlue

@Composable
fun VerifyScreen(onGoLogin: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    var resendMsg by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

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
                Icon(
                    imageVector = IconList.Email,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = Gold
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.verify_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold, color = Gold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.verify_sent_desc, auth.currentUser?.email ?: ""),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.verify_instruction),
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
                        Text(
                            text = stringResource(R.string.verify_button_done),
                            fontSize = 15.sp, color = Color.White
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    if (resendMsg.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSuccess) IconList.Done else IconList.Error,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = resendMsg,
                                fontSize = 13.sp,
                                color = if (isSuccess) NavyBlue else MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    OutlinedButton(
                        onClick = {
                            isSending = true
                            auth.currentUser?.sendEmailVerification()
                                ?.addOnSuccessListener {
                                    resendMsg = context.getString(R.string.verify_resend_success)
                                    isSuccess = true
                                    isSending = false
                                }
                                ?.addOnFailureListener {
                                    resendMsg = context.getString(R.string.common_error_resend)
                                    isSuccess = false
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
                        else {
                            Icon(IconList.Update, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.verify_button_resend), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
