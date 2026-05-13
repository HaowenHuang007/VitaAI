package com.vitaai.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.ui.theme.NavyBlue
import com.vitaai.app.utils.AchievementManager
import com.vitaai.app.utils.DateUtils
import com.vitaai.app.utils.LanguageManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun GoalsScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var startWeight by remember { mutableStateOf(0.0) }
    var currentWeight by remember { mutableStateOf(0.0) }
    var targetWeight by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("") }
    var savedMsg by remember { mutableStateOf("") }
    var existingTargetWeight by remember { mutableStateOf(0.0) }
    var existingTargetDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            currentWeight = doc.getDouble("weight") ?: 0.0
            startWeight = doc.getDouble("startWeight") ?: currentWeight
            existingTargetWeight = doc.getDouble("targetWeight") ?: 0.0
            existingTargetDate = doc.getString("targetDate") ?: ""
            if (existingTargetWeight > 0) targetWeight = existingTargetWeight.toString()
            if (existingTargetDate.isNotEmpty()) targetDate = existingTargetDate
        }
    }

    val parsedTargetWeight = targetWeight.toDoubleOrNull()
    val daysLeft = if (existingTargetDate.isNotEmpty()) {
        DateUtils.daysBetween(DateUtils.todayKey(), existingTargetDate)
    } else 0L

    val totalDelta = if (existingTargetWeight > 0 && startWeight > 0)
        abs(existingTargetWeight - startWeight) else 0.0
    val currentDelta = if (existingTargetWeight > 0 && startWeight > 0)
        abs(currentWeight - startWeight) else 0.0
    val progressPct = if (totalDelta > 0) (currentDelta / totalDelta).coerceIn(0.0, 1.0) else 0.0

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("🎯 ${LanguageManager.t("my_goals")}", fontSize = 24.sp,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))

        if (existingTargetWeight > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavyBlue)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(LanguageManager.t("current_goal"), fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f))
                    Text("${existingTargetWeight} kg", fontSize = 32.sp,
                        fontWeight = FontWeight.Bold, color = Gold)
                    if (existingTargetDate.isNotEmpty()) {
                        Text(
                            LanguageManager.t("days_left").replace("{days}", daysLeft.toString()),
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${LanguageManager.t("now")}: ${currentWeight} kg · ${LanguageManager.t("start")}: ${startWeight} kg",
                        fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressPct.toFloat())
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(Gold)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("${(progressPct * 100).toInt()}%", fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold)
                }
            }
            if (progressPct >= 1.0) {
                LaunchedEffect(Unit) { AchievementManager.unlock("goal_reached") }
            }
            Spacer(Modifier.height(20.dp))
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(LanguageManager.t("set_goal"), fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetWeight,
                    onValueChange = { new -> if (new.matches(Regex("^\\d{0,3}([.,]\\d{0,2})?$"))) targetWeight = new.replace(',', '.') },
                    label = { Text(LanguageManager.t("target_weight")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text(LanguageManager.t("target_date_hint")) },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 90).forEach { days ->
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_YEAR, days)
                                targetDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("+${days}d", fontSize = 12.sp) }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (parsedTargetWeight == null) return@Button
                        val data = mutableMapOf<String, Any>(
                            "targetWeight" to parsedTargetWeight,
                            "startWeight" to (if (startWeight == 0.0) currentWeight else startWeight)
                        )
                        if (targetDate.isNotEmpty()) data["targetDate"] = targetDate
                        db.collection("users").document(uid)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener {
                                savedMsg = "✅ ${LanguageManager.t("saved")}"
                                existingTargetWeight = parsedTargetWeight
                                existingTargetDate = targetDate
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = parsedTargetWeight != null && parsedTargetWeight in 20.0..300.0
                ) { Text(LanguageManager.t("save_goal"), fontSize = 16.sp) }

                if (savedMsg.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(savedMsg, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
