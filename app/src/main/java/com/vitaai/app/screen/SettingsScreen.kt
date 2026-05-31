package com.vitaai.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.ui.theme.NavyBlue
import com.vitaai.app.utils.LanguageManager
import com.vitaai.app.utils.NotificationScheduler
import com.vitaai.app.utils.ThemeManager

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onGoBMR: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val context = LocalContext.current

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    val currentLang by LanguageManager.currentLanguage
    val themeMode by ThemeManager.mode

    val (initWater, initEvening) = remember { NotificationScheduler.loadPrefs(context) }
    var waterNotif by remember { mutableStateOf(initWater) }
    var eveningNotif by remember { mutableStateOf(initEvening) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Info usuario
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("👤 ${LanguageManager.t("my_account")}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Email: ${user?.email ?: ""}", fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Selector de idioma
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🌍 ${LanguageManager.t("select_language")}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                LanguageManager.languages.forEach { (code, flag, name) ->
                    val isSelected = currentLang == code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) NavyBlue.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .clickable {
                                LanguageManager.setLanguage(context, code)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(flag, fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(name, fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NavyBlue else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f))
                        if (isSelected) {
                            Text("✓", fontSize = 18.sp, color = Gold,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    if (code != LanguageManager.languages.last().first) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ===== TEMA =====
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🎨 ${LanguageManager.t("theme")}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                listOf(
                    ThemeManager.MODE_SYSTEM to "theme_system",
                    ThemeManager.MODE_LIGHT to "theme_light",
                    ThemeManager.MODE_DARK to "theme_dark"
                ).forEach { (mode, key) ->
                    val isSel = themeMode == mode
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) NavyBlue.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { ThemeManager.setMode(context, mode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(when (mode) {
                            ThemeManager.MODE_LIGHT -> "☀️"
                            ThemeManager.MODE_DARK -> "🌙"
                            else -> "⚙️"
                        }, fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(LanguageManager.t(key), fontSize = 15.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) NavyBlue else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f))
                        if (isSel) Text("✓", fontSize = 18.sp, color = Gold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ===== NOTIFICACIONES =====
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🔔 ${LanguageManager.t("notifications")}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("💧 ${LanguageManager.t("notif_water_title")}", fontSize = 14.sp)
                        Text(LanguageManager.t("notif_water_desc"),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(checked = waterNotif, onCheckedChange = { enabled ->
                        waterNotif = enabled
                        NotificationScheduler.savePref(context, "notif_water", enabled)
                        NotificationScheduler.scheduleWaterReminder(context, enabled)
                    })
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🌙 ${LanguageManager.t("notif_evening_title")}", fontSize = 14.sp)
                        Text(LanguageManager.t("notif_evening_desc"),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(checked = eveningNotif, onCheckedChange = { enabled ->
                        eveningNotif = enabled
                        NotificationScheduler.savePref(context, "notif_evening", enabled)
                        NotificationScheduler.scheduleEveningReminder(context, enabled)
                    })
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ===== BMR CALCULATOR =====
        Card(
            onClick = onGoBMR,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("🧮", fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(LanguageManager.t("bmr_calculator"),
                        fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(LanguageManager.t("bmr_subtitle"),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Cambiar contraseña
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(LanguageManager.t("change_password"),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(LanguageManager.t("current_password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(LanguageManager.t("new_password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(LanguageManager.t("confirm_password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (errorMsg.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
                if (successMsg.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(successMsg, color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        errorMsg = ""
                        successMsg = ""
                        if (newPassword != confirmPassword) {
                            errorMsg = LanguageManager.t("pwd_no_match")
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            errorMsg = LanguageManager.t("pwd_too_short")
                            return@Button
                        }
                        isLoading = true
                        val credential = EmailAuthProvider.getCredential(
                            user?.email ?: "", currentPassword
                        )
                        user?.reauthenticate(credential)
                            ?.addOnSuccessListener {
                                user.updatePassword(newPassword)
                                    .addOnSuccessListener {
                                        successMsg = "✅ ${LanguageManager.t("update_password")} OK"
                                        currentPassword = ""
                                        newPassword = ""
                                        confirmPassword = ""
                                        isLoading = false
                                    }
                                    .addOnFailureListener {
                                        errorMsg = LanguageManager.t("pwd_update_failed")
                                        isLoading = false
                                    }
                            }
                            ?.addOnFailureListener {
                                errorMsg = LanguageManager.t("pwd_wrong_current")
                                isLoading = false
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && currentPassword.isNotEmpty()
                            && newPassword.isNotEmpty() && confirmPassword.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                ) {
                    if (isLoading) CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White, strokeWidth = 2.dp
                    )
                    else Text(LanguageManager.t("update_password"), fontSize = 16.sp,
                        color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}