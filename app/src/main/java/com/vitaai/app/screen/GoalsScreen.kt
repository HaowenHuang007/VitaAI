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

private object GoalsConstants {
    const val USERS_COLLECTION = "users"
    const val FIELD_WEIGHT = "weight"
    const val FIELD_START_WEIGHT = "startWeight"
    const val FIELD_TARGET_WEIGHT = "targetWeight"
    const val FIELD_TARGET_DATE = "targetDate"

    const val LANG_MY_GOALS = "my_goals"
    const val LANG_CURRENT_GOAL = "current_goal"
    const val LANG_DAYS_LEFT = "days_left"
    const val LANG_NOW = "now"
    const val LANG_START = "start"
    const val LANG_SET_GOAL = "set_goal"
    const val LANG_TARGET_WEIGHT = "target_weight"
    const val LANG_TARGET_DATE_HINT = "target_date_hint"
    const val LANG_SAVED = "saved"
    const val LANG_SAVE_GOAL = "save_goal"

    const val PLACEHOLDER_DAYS = "{days}"
    const val DATE_FORMAT = "yyyy-MM-dd"
    const val DATE_PLACEHOLDER = "YYYY-MM-DD"
    const val UNIT_KG = " kg"
    const val PROGRESS_FORMAT = "%d%%"
    
    const val WEIGHT_REGEX = "^\\d{0,3}([.,]\\d{0,2})?$"
    const val COMMA = ","
    const val DOT = "."

    const val ACHIEVEMENT_GOAL_REACHED = "goal_reached"
}

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
        db.collection(GoalsConstants.USERS_COLLECTION).document(uid).get().addOnSuccessListener { doc ->
            currentWeight = doc.getDouble(GoalsConstants.FIELD_WEIGHT) ?: 0.0
            startWeight = doc.getDouble(GoalsConstants.FIELD_START_WEIGHT) ?: currentWeight
            existingTargetWeight = doc.getDouble(GoalsConstants.FIELD_TARGET_WEIGHT) ?: 0.0
            existingTargetDate = doc.getString(GoalsConstants.FIELD_TARGET_DATE) ?: ""
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
        // Emoji removed: 🎯
        Text(LanguageManager.t(GoalsConstants.LANG_MY_GOALS), fontSize = 24.sp,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))

        if (existingTargetWeight > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavyBlue)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(LanguageManager.t(GoalsConstants.LANG_CURRENT_GOAL), fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f))
                    Text("${existingTargetWeight}${GoalsConstants.UNIT_KG}", fontSize = 32.sp,
                        fontWeight = FontWeight.Bold, color = Gold)
                    if (existingTargetDate.isNotEmpty()) {
                        Text(
                            LanguageManager.t(GoalsConstants.LANG_DAYS_LEFT).replace(GoalsConstants.PLACEHOLDER_DAYS, daysLeft.toString()),
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${LanguageManager.t(GoalsConstants.LANG_NOW)}: ${currentWeight}${GoalsConstants.UNIT_KG} · ${LanguageManager.t(GoalsConstants.LANG_START)}: ${startWeight}${GoalsConstants.UNIT_KG}",
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
                    Text(GoalsConstants.PROGRESS_FORMAT.format((progressPct * 100).toInt()), fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold)
                }
            }
            if (progressPct >= 1.0) {
                LaunchedEffect(Unit) { AchievementManager.unlock(GoalsConstants.ACHIEVEMENT_GOAL_REACHED) }
            }
            Spacer(Modifier.height(20.dp))
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(LanguageManager.t(GoalsConstants.LANG_SET_GOAL), fontSize = 16.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetWeight,
                    onValueChange = { new -> if (new.matches(Regex(GoalsConstants.WEIGHT_REGEX))) targetWeight = new.replace(GoalsConstants.COMMA, GoalsConstants.DOT) },
                    label = { Text(LanguageManager.t(GoalsConstants.LANG_TARGET_WEIGHT)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text(LanguageManager.t(GoalsConstants.LANG_TARGET_DATE_HINT)) },
                    placeholder = { Text(GoalsConstants.DATE_PLACEHOLDER) },
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
                                targetDate = SimpleDateFormat(GoalsConstants.DATE_FORMAT, Locale.US).format(cal.time)
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
                            GoalsConstants.FIELD_TARGET_WEIGHT to parsedTargetWeight,
                            GoalsConstants.FIELD_START_WEIGHT to (if (startWeight == 0.0) currentWeight else startWeight)
                        )
                        if (targetDate.isNotEmpty()) data[GoalsConstants.FIELD_TARGET_DATE] = targetDate
                        db.collection(GoalsConstants.USERS_COLLECTION).document(uid)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener {
                                // Emoji removed: ✅
                                savedMsg = LanguageManager.t(GoalsConstants.LANG_SAVED)
                                existingTargetWeight = parsedTargetWeight
                                existingTargetDate = targetDate
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = parsedTargetWeight != null && parsedTargetWeight in 20.0..300.0
                ) { Text(LanguageManager.t(GoalsConstants.LANG_SAVE_GOAL), fontSize = 16.sp) }

                if (savedMsg.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(savedMsg, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
