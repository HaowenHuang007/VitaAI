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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vitaai.app.utils.AchievementManager
import com.vitaai.app.utils.DateUtils
import com.vitaai.app.utils.LanguageManager
import com.vitaai.app.utils.StreakManager
import com.vitaai.app.utils.XPManager
import java.text.SimpleDateFormat
import java.util.*

private object ExerciseConstants {
    const val USERS_COLLECTION = "users"
    const val EXERCISES_COLLECTION = "exercises"
    const val FIELD_WEIGHT = "weight"
    const val FIELD_TIMESTAMP = "timestamp"
    const val FIELD_TYPE_KEY = "typeKey"
    const val FIELD_EMOJI = "emoji"
    const val FIELD_DURATION = "durationMin"
    const val FIELD_CALORIES = "caloriesBurned"
    const val FIELD_DATE = "date"
    const val FIELD_TIME = "time"

    const val LANG_EXERCISE_LOG = "exercise_log"
    const val LANG_CHOOSE_EXERCISE = "choose_exercise"
    const val LANG_DURATION_MIN = "duration_min"
    const val LANG_TIME = "exercise_time"
    const val LANG_SAVED = "saved"
    const val LANG_SAVE = "save"
    const val LANG_RECENT_HISTORY = "recent_history"
    
    const val EX_PREFIX = "ex_"
    const val KCAL_UNIT = " kcal"
    const val APPROX_SYMBOL = "≈ "
    const val HISTORY_SEP = " min · "

    const val ACTION_EXERCISE = "exercise"
    const val ACHIEVEMENT_FIRST_EXERCISE = "first_exercise"
}

private data class ExerciseType(val key: String, val emoji: String, val metPerMin: Double)

private val EXERCISE_TYPES = listOf(
    // TODO: This string is primarily an emoji
    ExerciseType("walking", "🚶", 3.5 / 60),
    // TODO: This string is primarily an emoji
    ExerciseType("running", "🏃", 9.8 / 60),
    // TODO: This string is primarily an emoji
    ExerciseType("cycling", "🚴", 7.5 / 60),
    // TODO: This string is primarily an emoji
    ExerciseType("swimming", "🏊", 8.0 / 60),
    // TODO: This string is primarily an emoji
    ExerciseType("yoga", "🧘", 2.5 / 60),
    // TODO: This string is primarily an emoji
    ExerciseType("gym", "🏋️", 6.0 / 60),
    // TODO: This string is primarily an emoji
    ExerciseType("dance", "💃", 5.0 / 60),
    // TODO: This string is primarily an emoji
    ExerciseType("hiit", "🔥", 10.0 / 60)
)

data class ExerciseEntry(
    val id: String,
    val type: String,
    val emoji: String,
    val durationMin: Int,
    val caloriesBurned: Int,
    val date: String,
    val time: String
)

@Composable
fun ExerciseScreen(modifier: Modifier = Modifier) {
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
        db.collection(ExerciseConstants.USERS_COLLECTION).document(uid).get().addOnSuccessListener {
            weight = it.getDouble(ExerciseConstants.FIELD_WEIGHT) ?: 70.0
        }
        db.collection(ExerciseConstants.USERS_COLLECTION).document(uid).collection(ExerciseConstants.EXERCISES_COLLECTION)
            .orderBy(ExerciseConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                entries = snap?.documents?.map { d ->
                    ExerciseEntry(
                        id = d.id,
                        type = d.getString(ExerciseConstants.FIELD_TYPE_KEY) ?: "",
                        emoji = d.getString(ExerciseConstants.FIELD_EMOJI) ?: "",
                        durationMin = (d.getLong(ExerciseConstants.FIELD_DURATION) ?: 0).toInt(),
                        caloriesBurned = (d.getLong(ExerciseConstants.FIELD_CALORIES) ?: 0).toInt(),
                        date = d.getString(ExerciseConstants.FIELD_DATE) ?: "",
                        time = d.getString(ExerciseConstants.FIELD_TIME) ?: ""
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
        // Emoji removed: 🏋️
        Text(LanguageManager.t(ExerciseConstants.LANG_EXERCISE_LOG),
            fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))

        Text(LanguageManager.t(ExerciseConstants.LANG_CHOOSE_EXERCISE), fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold)
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
                        // TODO: This string is primarily an emoji
                        Text(type.emoji, fontSize = 28.sp)
                        Text(LanguageManager.t(ExerciseConstants.EX_PREFIX + type.key), fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
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
                label = { Text(LanguageManager.t(ExerciseConstants.LANG_DURATION_MIN)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = exerciseTime,
                onValueChange = { exerciseTime = it },
                label = { Text(LanguageManager.t(ExerciseConstants.LANG_TIME)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
        }

        if (estimatedCals > 0) {
            Spacer(Modifier.height(8.dp))
            Text("${ExerciseConstants.APPROX_SYMBOL}$estimatedCals${ExerciseConstants.KCAL_UNIT}", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val t = selected ?: return@Button
                val d = parsedDuration ?: return@Button
                db.collection(ExerciseConstants.USERS_COLLECTION).document(uid).collection(ExerciseConstants.EXERCISES_COLLECTION)
                    .add(mapOf(
                        ExerciseConstants.FIELD_TYPE_KEY to t.key,
                        ExerciseConstants.FIELD_EMOJI to t.emoji,
                        ExerciseConstants.FIELD_DURATION to d,
                        ExerciseConstants.FIELD_CALORIES to estimatedCals,
                        ExerciseConstants.FIELD_DATE to DateUtils.todayKey(),
                        ExerciseConstants.FIELD_TIME to exerciseTime,
                        ExerciseConstants.FIELD_TIMESTAMP to System.currentTimeMillis()
                    )).addOnSuccessListener {
                        // Emoji removed: ✅
                        savedMsg = "${LanguageManager.t(ExerciseConstants.LANG_SAVED)} · $estimatedCals${ExerciseConstants.KCAL_UNIT}"
                        duration = ""
                        selected = null
                        StreakManager.recordActivity()
                        AchievementManager.unlock(ExerciseConstants.ACHIEVEMENT_FIRST_EXERCISE)
                        XPManager.addXPWithLimit(
                            amount = 10, actionKey = ExerciseConstants.ACTION_EXERCISE, dailyLimit = 3
                        )
                    }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = selected != null && (parsedDuration ?: 0) in 1..600
        ) { Text(LanguageManager.t(ExerciseConstants.LANG_SAVE), fontSize = 16.sp) }

        if (savedMsg.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(savedMsg, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(24.dp))
        Text(LanguageManager.t(ExerciseConstants.LANG_RECENT_HISTORY), fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold)
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
                    // TODO: This string is primarily an emoji
                    Text(e.emoji, fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(LanguageManager.t("${ExerciseConstants.EX_PREFIX}${e.type}"),
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${e.durationMin}${ExerciseConstants.HISTORY_SEP}${e.date} ${e.time}",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Text("${e.caloriesBurned}${ExerciseConstants.KCAL_UNIT}", fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
