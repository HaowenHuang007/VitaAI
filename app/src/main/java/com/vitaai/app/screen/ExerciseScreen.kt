package com.vitaai.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.utils.AchievementManager
import com.vitaai.app.utils.DateUtils
import com.vitaai.app.utils.StreakManager
import com.vitaai.app.utils.XPManager
import java.text.SimpleDateFormat
import java.util.*

private const val USERS_COLLECTION = "users"
private const val EXERCISES_COLLECTION = "exercises"
private const val FIELD_WEIGHT = "weight"
private const val FIELD_TIMESTAMP = "timestamp"
private const val FIELD_TYPE_KEY = "typeKey"
private const val FIELD_DURATION = "durationMin"
private const val FIELD_CALORIES = "caloriesBurned"
private const val FIELD_DATE = "date"
private const val FIELD_TIME = "time"

private const val ACTION_EXERCISE = "exercise"
private const val ACHIEVEMENT_FIRST_EXERCISE = "first_exercise"

private data class ExerciseType(val key: String, val metPerMin: Double)

private val EXERCISE_TYPES = listOf(
    ExerciseType("walking", 3.5 / 60),
    ExerciseType("running", 9.8 / 60),
    ExerciseType("cycling", 7.5 / 60),
    ExerciseType("swimming", 8.0 / 60),
    ExerciseType("yoga", 2.5 / 60),
    ExerciseType("gym", 6.0 / 60),
    ExerciseType("dance", 5.0 / 60),
    ExerciseType("hiit", 10.0 / 60)
)

data class ExerciseEntry(
    val id: String,
    val type: String,
    val durationMin: Int,
    val caloriesBurned: Int,
    val date: String,
    val time: String
)

@Composable
fun ExerciseScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var selected by remember { mutableStateOf<ExerciseType?>(null) }
    var duration by remember { mutableStateOf("") }
    var exerciseTime by remember { 
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) 
    }
    var weight by remember { mutableStateOf(70.0) }
    var savedMsg by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<ExerciseEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection(USERS_COLLECTION).document(uid).get().addOnSuccessListener {
            weight = it.getDouble(FIELD_WEIGHT) ?: 70.0
        }
        db.collection(USERS_COLLECTION).document(uid).collection(EXERCISES_COLLECTION)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                entries = snap?.documents?.map { d ->
                    ExerciseEntry(
                        id = d.id,
                        type = d.getString(FIELD_TYPE_KEY) ?: "",
                        durationMin = (d.getLong(FIELD_DURATION) ?: 0).toInt(),
                        caloriesBurned = (d.getLong(FIELD_CALORIES) ?: 0).toInt(),
                        date = d.getString(FIELD_DATE) ?: "",
                        time = d.getString(FIELD_TIME) ?: ""
                    )
                } ?: emptyList()
            }
    }

    val parsedDuration = duration.toIntOrNull()
    val estimatedCals = if (selected != null && parsedDuration != null)
        (selected!!.metPerMin * parsedDuration * weight).toInt() else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.exercise_log_title),
            fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.exercise_choose),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        EXERCISE_TYPES.chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { type ->
                    val isSel = selected?.key == type.key
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSel) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selected = type }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = IconList.getIconForExercise(type.key),
                            contentDescription = null,
                            tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = stringResource(id = when(type.key) {
                                "walking" -> R.string.ex_walking
                                "running" -> R.string.ex_running
                                "cycling" -> R.string.ex_cycling
                                "swimming" -> R.string.ex_swimming
                                "yoga" -> R.string.ex_yoga
                                "gym" -> R.string.ex_gym
                                "dance" -> R.string.ex_dance
                                "hiit" -> R.string.ex_hiit
                                else -> R.string.exercise_log_title
                            }),
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = duration,
                onValueChange = { new -> if (new.all { it.isDigit() } && new.length <= 3) duration = new },
                label = { Text(stringResource(R.string.exercise_duration_min)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = exerciseTime,
                onValueChange = { exerciseTime = it },
                label = { Text(stringResource(R.string.exercise_duration_min)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
        }

        if (estimatedCals > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.exercise_approx_format, estimatedCals),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val t = selected ?: return@Button
                val d = parsedDuration ?: return@Button
                db.collection(USERS_COLLECTION).document(uid).collection(EXERCISES_COLLECTION)
                    .add(mapOf(
                        FIELD_TYPE_KEY to t.key,
                        FIELD_DURATION to d,
                        FIELD_CALORIES to estimatedCals,
                        FIELD_DATE to DateUtils.todayKey(),
                        FIELD_TIME to exerciseTime,
                        FIELD_TIMESTAMP to System.currentTimeMillis()
                    )).addOnSuccessListener {
                        savedMsg = context.getString(R.string.exercise_saved_format, estimatedCals)
                        duration = ""
                        selected = null
                        StreakManager.recordActivity()
                        AchievementManager.unlock(ACHIEVEMENT_FIRST_EXERCISE)
                        XPManager.addXPWithLimit(
                            amount = 10, actionKey = ACTION_EXERCISE, dailyLimit = 3
                        )
                    }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = selected != null && (parsedDuration ?: 0) in 1..600
        ) { 
            Text(stringResource(R.string.common_save), fontSize = 16.sp) 
        }

        if (savedMsg.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(savedMsg, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.exercise_recent_history),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        entries.forEach { e ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = IconList.getIconForExercise(e.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = when(e.type) {
                                "walking" -> R.string.ex_walking
                                "running" -> R.string.ex_running
                                "cycling" -> R.string.ex_cycling
                                "swimming" -> R.string.ex_swimming
                                "yoga" -> R.string.ex_yoga
                                "gym" -> R.string.ex_gym
                                "dance" -> R.string.ex_dance
                                "hiit" -> R.string.ex_hiit
                                else -> R.string.exercise_log_title
                            }),
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.exercise_history_item_format, e.durationMin, "${e.date} ${e.time}"),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = "${e.caloriesBurned} ${stringResource(R.string.common_kcal_unit)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
