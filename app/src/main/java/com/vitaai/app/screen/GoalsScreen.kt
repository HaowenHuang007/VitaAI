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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.ui.theme.NavyBlue
import com.vitaai.app.utils.AchievementManager
import com.vitaai.app.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

private const val USERS_COLLECTION = "users"
private const val FIELD_WEIGHT = "weight"
private const val FIELD_START_WEIGHT = "startWeight"
private const val FIELD_TARGET_WEIGHT = "targetWeight"
private const val FIELD_TARGET_DATE = "targetDate"

private const val DATE_FORMAT = "yyyy-MM-dd"
private const val ACHIEVEMENT_GOAL_REACHED = "goal_reached"

@Composable
fun GoalsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var startWeight by remember { mutableStateOf(0.0) }
    var currentWeight by remember { mutableStateOf(0.0) }
    var targetWeightInput by remember { mutableStateOf("") }
    var targetDateInput by remember { mutableStateOf("") }
    var savedMsg by remember { mutableStateOf("") }
    var existingTargetWeight by remember { mutableStateOf(0.0) }
    var existingTargetDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection(USERS_COLLECTION).document(uid).get().addOnSuccessListener { doc ->
            currentWeight = doc.getDouble(FIELD_WEIGHT) ?: 0.0
            startWeight = doc.getDouble(FIELD_START_WEIGHT) ?: currentWeight
            existingTargetWeight = doc.getDouble(FIELD_TARGET_WEIGHT) ?: 0.0
            existingTargetDate = doc.getString(FIELD_TARGET_DATE) ?: ""
            if (existingTargetWeight > 0) targetWeightInput = existingTargetWeight.toString()
            if (existingTargetDate.isNotEmpty()) targetDateInput = existingTargetDate
        }
    }

    val parsedTargetWeight = targetWeightInput.toDoubleOrNull()
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = IconList.Target,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.goals_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(16.dp))

        if (existingTargetWeight > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavyBlue)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.goals_current),
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(R.string.goals_weight_format, existingTargetWeight),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                    if (existingTargetDate.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.goals_days_left, daysLeft),
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${stringResource(R.string.goals_now)}: ${currentWeight}kg · ${stringResource(R.string.goals_start)}: ${startWeight}kg",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
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
                    Text(
                        text = "${(progressPct * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (progressPct >= 1.0) {
                LaunchedEffect(Unit) { AchievementManager.unlock(ACHIEVEMENT_GOAL_REACHED) }
            }
            Spacer(Modifier.height(20.dp))
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = IconList.Goal, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.goals_set),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetWeightInput,
                    onValueChange = { new -> 
                        if (new.matches(Regex("^\\d{0,3}([.,]\\d{0,2})?$"))) {
                            targetWeightInput = new.replace(',', '.') 
                        }
                    },
                    label = { Text(stringResource(R.string.goals_target_weight)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = targetDateInput,
                    onValueChange = { targetDateInput = it },
                    label = { Text(stringResource(R.string.goals_target_date_hint)) },
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
                                targetDateInput = SimpleDateFormat(DATE_FORMAT, Locale.US).format(cal.time)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("+${days}d", fontSize = 12.sp) }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        val weight = parsedTargetWeight ?: return@Button
                        val data = mutableMapOf<String, Any>(
                            FIELD_TARGET_WEIGHT to weight,
                            FIELD_START_WEIGHT to (if (startWeight == 0.0) currentWeight else startWeight)
                        )
                        if (targetDateInput.isNotEmpty()) data[FIELD_TARGET_DATE] = targetDateInput
                        db.collection(USERS_COLLECTION).document(uid)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener {
                                savedMsg = context.getString(R.string.common_saved)
                                existingTargetWeight = weight
                                existingTargetDate = targetDateInput
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = parsedTargetWeight != null && parsedTargetWeight in 20.0..300.0
                ) {
                    Icon(imageVector = IconList.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.goals_save_button), fontSize = 16.sp)
                }

                if (savedMsg.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(savedMsg, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
