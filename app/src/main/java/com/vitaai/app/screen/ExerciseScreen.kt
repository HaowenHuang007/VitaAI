package com.vitaai.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

private data class ExerciseType(val key: String, val emoji: String, val metPerMin: Double)

private val EXERCISE_TYPES = listOf(
    ExerciseType("walking", "🚶", 3.5 / 60),
    ExerciseType("running", "🏃", 9.8 / 60),
    ExerciseType("cycling", "🚴", 7.5 / 60),
    ExerciseType("swimming", "🏊", 8.0 / 60),
    ExerciseType("yoga", "🧘", 2.5 / 60),
    ExerciseType("gym", "🏋️", 6.0 / 60),
    ExerciseType("dance", "💃", 5.0 / 60),
    ExerciseType("hiit", "🔥", 10.0 / 60)
)

data class ExerciseEntry(
    val id: String,
    val type: String,
    val emoji: String,
    val durationMin: Int,
    val caloriesBurned: Int,
    val date: String
)

@Composable
fun ExerciseScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var selected by remember { mutableStateOf<ExerciseType?>(null) }
    var duration by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf(70.0) }
    var savedMsg by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<ExerciseEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener {
            weight = it.getDouble("weight") ?: 70.0
        }
        db.collection("users").document(uid).collection("exercises")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snap, _ ->
                entries = snap?.documents?.map { d ->
                    ExerciseEntry(
                        id = d.id,
                        type = d.getString("typeKey") ?: "",
                        emoji = d.getString("emoji") ?: "",
                        durationMin = (d.getLong("durationMin") ?: 0).toInt(),
                        caloriesBurned = (d.getLong("caloriesBurned") ?: 0).toInt(),
                        date = d.getString("date") ?: ""
                    )
                } ?: emptyList()
            }
    }

    val parsedDuration = duration.toIntOrNull()
    val estimatedCals = if (selected != null && parsedDuration != null)
        (selected!!.metPerMin * parsedDuration * weight).toInt() else 0

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("🏋️ ${LanguageManager.t("exercise_log")}",
            fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))

        Text(LanguageManager.t("choose_exercise"), fontSize = 14.sp,
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
                        Text(type.emoji, fontSize = 28.sp)
                        Text(LanguageManager.t("ex_" + type.key), fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = duration,
            onValueChange = { new -> if (new.all { it.isDigit() } && new.length <= 3) duration = new },
            label = { Text(LanguageManager.t("duration_min")) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        if (estimatedCals > 0) {
            Spacer(Modifier.height(8.dp))
            Text("≈ $estimatedCals kcal", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val t = selected ?: return@Button
                val d = parsedDuration ?: return@Button
                db.collection("users").document(uid).collection("exercises")
                    .add(mapOf(
                        "typeKey" to t.key,
                        "emoji" to t.emoji,
                        "durationMin" to d,
                        "caloriesBurned" to estimatedCals,
                        "date" to DateUtils.todayKey(),
                        "timestamp" to System.currentTimeMillis()
                    )).addOnSuccessListener {
                        savedMsg = "✅ ${LanguageManager.t("saved")} · $estimatedCals kcal"
                        duration = ""
                        selected = null
                        StreakManager.recordActivity()
                        AchievementManager.unlock("first_exercise")
                        XPManager.addXPWithLimit(
                            amount = 10, actionKey = "exercise", dailyLimit = 3
                        )
                    }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = selected != null && (parsedDuration ?: 0) in 1..600
        ) { Text(LanguageManager.t("save"), fontSize = 16.sp) }

        if (savedMsg.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(savedMsg, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(24.dp))
        Text(LanguageManager.t("recent_history"), fontSize = 14.sp,
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
                    Text(e.emoji, fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(LanguageManager.t("ex_${e.type}"),
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${e.durationMin} min · ${e.date}",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Text("${e.caloriesBurned} kcal", fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
