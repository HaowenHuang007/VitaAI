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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
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

    var currentPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    val themeMode by ThemeManager.mode
    val currentLang by LanguageManager.currentLanguage

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(IconList.Profile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_my_account), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("${stringResource(R.string.common_email)}: ${user?.email ?: ""}", fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(Modifier.height(24.dp))

        // ===== TEMA =====
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(IconList.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_theme), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                listOf(
                    ThemeManager.MODE_SYSTEM to R.string.settings_theme_system,
                    ThemeManager.MODE_LIGHT to R.string.settings_theme_light,
                    ThemeManager.MODE_DARK to R.string.settings_theme_dark
                ).forEach { (mode, resId) ->
                    val isSel = themeMode == mode
                    val itemBg = if (isSel) NavyBlue.copy(alpha = 0.1f) else Color.Transparent
                    val itemColor = if (isSel) NavyBlue else MaterialTheme.colorScheme.onSurface
                    val itemWeight = if (isSel) FontWeight.Bold else FontWeight.Normal

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(itemBg)
                            .clickable { ThemeManager.setMode(context, mode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (mode) {
                                ThemeManager.MODE_LIGHT -> IconList.LightMode
                                ThemeManager.MODE_DARK -> IconList.DarkMode
                                else -> IconList.SystemMode
                            },
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = itemColor
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(resId),
                            fontSize = 15.sp,
                            fontWeight = itemWeight,
                            color = itemColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSel) Icon(IconList.Done, null, tint = Gold, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ===== IDIOMA =====
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(IconList.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_language), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                listOf(
                    LanguageManager.LANG_ES to R.string.lang_es,
                    LanguageManager.LANG_EN to R.string.lang_en,
                    LanguageManager.LANG_PT to R.string.lang_pt,
                    LanguageManager.LANG_FR to R.string.lang_fr,
                    LanguageManager.LANG_ZH to R.string.lang_zh
                ).forEach { (langCode, resId) ->
                    val isSel = currentLang == langCode
                    val itemBg = if (isSel) NavyBlue.copy(alpha = 0.1f) else Color.Transparent
                    val itemColor = if (isSel) NavyBlue else MaterialTheme.colorScheme.onSurface
                    val itemWeight = if (isSel) FontWeight.Bold else FontWeight.Normal

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(itemBg)
                            .clickable { LanguageManager.setLanguage(context, langCode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(resId),
                            fontSize = 15.sp,
                            fontWeight = itemWeight,
                            color = itemColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSel) Icon(IconList.Done, null, tint = Gold, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ===== NOTIFICACIONES =====
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(IconList.Notification, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_notifications), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(IconList.Water, null, tint = IconList.WaterBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.settings_notif_water_title), fontSize = 14.sp)
                        }
                        Text(stringResource(R.string.settings_notif_water_desc),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(IconList.DarkMode, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.settings_notif_evening_title), fontSize = 14.sp)
                        }
                        Text(stringResource(R.string.settings_notif_evening_desc),
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
                Icon(IconList.Calculate, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_bmr_calculator),
                        fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.settings_bmr_subtitle),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Icon(IconList.Update, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Cambiar contraseña
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(IconList.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_change_password), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = currentPasswordInput,
                    onValueChange = { currentPasswordInput = it },
                    label = { Text(stringResource(R.string.settings_current_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPasswordInput,
                    onValueChange = { newPasswordInput = it },
                    label = { Text(stringResource(R.string.settings_new_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPasswordInput,
                    onValueChange = { confirmPasswordInput = it },
                    label = { Text(stringResource(R.string.settings_confirm_password)) },
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
                        if (newPasswordInput != confirmPasswordInput) {
                            errorMsg = context.getString(R.string.settings_pwd_no_match)
                            return@Button
                        }
                        if (newPasswordInput.length < 6) {
                            errorMsg = context.getString(R.string.settings_pwd_too_short)
                            return@Button
                        }
                        isLoading = true
                        val credential = EmailAuthProvider.getCredential(user?.email ?: "", currentPasswordInput)
                        user?.reauthenticate(credential)
                            ?.addOnSuccessListener {
                                user.updatePassword(newPasswordInput)
                                    .addOnSuccessListener {
                                        successMsg = context.getString(R.string.settings_update_password_success)
                                        currentPasswordInput = ""
                                        newPasswordInput = ""
                                        confirmPasswordInput = ""
                                        isLoading = false
                                    }
                                    .addOnFailureListener {
                                        errorMsg = context.getString(R.string.settings_pwd_update_failed)
                                        isLoading = false
                                    }
                            }
                            ?.addOnFailureListener {
                                errorMsg = context.getString(R.string.settings_pwd_wrong_current)
                                isLoading = false
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && currentPasswordInput.isNotEmpty()
                            && newPasswordInput.isNotEmpty() && confirmPasswordInput.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                ) {
                    if (isLoading) CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White, strokeWidth = 2.dp
                    )
                    else Text(stringResource(R.string.settings_update_password), fontSize = 16.sp, color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
