package com.vitaai.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.utils.DateUtils
import com.vitaai.app.utils.callOpenAI
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

@Composable
fun WeeklyReportScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var isLoading by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var hasData by remember { mutableStateOf(false) }

    val locale = Locale.getDefault()
    val langName = locale.displayLanguage

    suspend fun generate() {
        val sevenDaysAgo = DateUtils.dateKey(-6)
        val today = DateUtils.todayKey()

        val nutritionData = try {
            db.collection("users").document(uid).collection("nutrition")
                .whereGreaterThanOrEqualTo("date", sevenDaysAgo).get().await()
                .documents.joinToString("\n") {
                    "${it.getString("date")}: ${it.getLong("calories") ?: 0} kcal, " +
                        "P${it.getLong("proteinG") ?: 0}, C${it.getLong("carbsG") ?: 0}, F${it.getLong("fatG") ?: 0}"
                }
        } catch (e: Exception) { "" }

        val moodData = try {
            db.collection("users").document(uid).collection("moods")
                .whereGreaterThanOrEqualTo("date", sevenDaysAgo).get().await()
                .documents.joinToString("; ") { "${it.getString("date")}: ${it.getString("mood")}" }
        } catch (e: Exception) { "" }

        val progressData = try {
            db.collection("users").document(uid).collection("progress")
                .whereGreaterThanOrEqualTo("date", sevenDaysAgo).get().await()
                .documents.joinToString("; ") {
                    "${it.getString("date")}: ${it.getDouble("weight")}kg"
                }
        } catch (e: Exception) { "" }

        val exerciseData = try {
            db.collection("users").document(uid).collection("exercises")
                .whereGreaterThanOrEqualTo("date", sevenDaysAgo).get().await()
                .documents.joinToString("; ") {
                    "${it.getString("date")}: ${it.getString("typeKey")} ${it.getLong("durationMin")}min"
                }
        } catch (e: Exception) { "" }

        hasData = nutritionData.isNotBlank() || moodData.isNotBlank() ||
            progressData.isNotBlank() || exerciseData.isNotBlank()

        if (!hasData) {
            report = "" // Handled in UI
            return
        }

        val prompt = """
            You are a personal health coach. Analyze this user's last 7 days ($sevenDaysAgo to $today).
            Respond in $langName. Return STRICT JSON:
            {
              "summary": "1-sentence headline of how the week went",
              "report": "detailed analysis with: nutrition trends, mood patterns, weight progress, exercise consistency, top 3 wins, top 2 things to improve, and a specific action plan for next week. Use short paragraphs."
            }

            NUTRITION: $nutritionData
            MOOD: $moodData
            WEIGHT: $progressData
            EXERCISE: $exerciseData
        """.trimIndent()

        val raw = callOpenAI(prompt)
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        try {
            val json = org.json.JSONObject(cleaned)
            summary = json.optString("summary", "")
            report = json.optString("report", cleaned)
        } catch (e: Exception) {
            report = cleaned
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        try { generate() } catch (e: Exception) {
            report = "Error: ${e.message}"
        }
        isLoading = false
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = IconList.WeeklyReport,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.report_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "${DateUtils.dateKey(-6)} → ${DateUtils.todayKey()}",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(top = 64.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.report_analyzing),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else if (!hasData && report.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 64.dp),
                contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.report_no_data),
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            if (summary.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(summary, modifier = Modifier.padding(16.dp),
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
            }
            if (report.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(report, modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp, lineHeight = 22.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    isLoading = true; summary = ""; report = ""
                    scope.launch {
                        try { generate() } catch (e: Exception) {
                            report = "Error: ${e.message}"
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(IconList.Update, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.common_regenerate), fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
