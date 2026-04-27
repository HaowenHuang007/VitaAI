package com.vitaai.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.ui.theme.NavyBlue
import com.vitaai.app.utils.LanguageManager
import com.vitaai.app.utils.XPManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun PlanScreen(
    modifier: Modifier = Modifier,
    age: Int, weight: Double, height: Double,
    goal: String, activity: String,
    onGoHome: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var dietPlan by remember { mutableStateOf("") }
    var exercisePlan by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaved by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val imc = weight / ((height / 100) * (height / 100))
    val imcFormatted = "%.1f".format(imc)
    val langName = when(LanguageManager.currentLanguage.value) {
        "en" -> "English"
        "zh" -> "Chinese"
        "fr" -> "French"
        "pt" -> "Portuguese"
        else -> "Spanish"
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val prompt = """
                    You are an expert nutritionist and personal trainer. Create a 100% personalized plan.
                    RESPOND ONLY IN $langName.
                    
                    USER DATA:
                    - Age: $age years
                    - Weight: ${weight}kg
                    - Height: ${height}cm
                    - BMI: $imcFormatted
                    - Main goal: $goal
                    - Activity level: $activity
                    
                    IMPORTANT:
                    - Plan MUST be specific to this data, NOT generic
                    - Mention weight (${weight}kg) and goal ($goal) explicitly
                    - Include exact daily calories, protein, carbs and fat in grams
                    - For exercise: specific days, sets, reps and duration
                    - Give concrete meal examples for breakfast, lunch, dinner and snacks
                    
                    Respond ONLY in this exact JSON format. Values must be PLAIN TEXT, no nested JSON:
                    {"diet": "diet plan here in plain text", "exercise": "exercise plan here in plain text"}
                """.trimIndent()

                val result = callOpenAI(prompt)
                val cleanResult = result.trim()
                    .removePrefix("```json").removePrefix("```")
                    .removeSuffix("```").trim()

                val json = JSONObject(cleanResult)
                val rawDiet = json.getString("diet")
                val rawExercise = json.getString("exercise")

                dietPlan = if (rawDiet.trimStart().startsWith("{") || rawDiet.trimStart().startsWith("[")) {
                    rawDiet.replace(Regex("[{}\\[\\]]"), "").replace("\"", "")
                        .replace(",", "\n").replace(":", ": ")
                        .lines().filter { it.trim().isNotEmpty() }.joinToString("\n").trim()
                } else rawDiet

                exercisePlan = if (rawExercise.trimStart().startsWith("{") || rawExercise.trimStart().startsWith("[")) {
                    rawExercise.replace(Regex("[{}\\[\\]]"), "").replace("\"", "")
                        .replace(",", "\n").replace(":", ": ")
                        .lines().filter { it.trim().isNotEmpty() }.joinToString("\n").trim()
                } else rawExercise

                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .update(mapOf(
                            "dietPlan" to dietPlan,
                            "exercisePlan" to exercisePlan,
                            "age" to age, "weight" to weight,
                            "height" to height, "goal" to goal, "imc" to imc
                        ))
                    XPManager.addXPWithLimit(
                        amount = XPManager.XP_GENERATE_PLAN,
                        actionKey = XPManager.KEY_PLAN,
                        dailyLimit = 1
                    ) { isSaved = true }
                }
            } catch (e: Exception) {
                errorMsg = "Error: ${e.message}"
            }
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(LanguageManager.t("current_plan"), fontSize = 24.sp,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("$age · ${weight}kg · ${height}cm · BMI $imcFormatted",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(24.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(LanguageManager.t("generating_plan"),
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center)
                }
            }
        } else if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = MaterialTheme.colorScheme.error)
        } else {
            if (isSaved) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("✅", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(LanguageManager.t("saved"),
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("+${XPManager.XP_GENERATE_PLAN} XP 🏆",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(LanguageManager.t("diet_plan"), fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(dietPlan, fontSize = 14.sp, lineHeight = 22.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(LanguageManager.t("exercise_plan"), fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(exercisePlan, fontSize = 14.sp, lineHeight = 22.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onGoHome,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
            ) {
                Text(LanguageManager.t("back_to_home"), fontSize = 16.sp, color = Color.White)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

suspend fun callOpenAI(prompt: String): String = withContext(Dispatchers.IO) {
    val apiKey = "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"
    val url = URL("https://api.openai.com/v1/chat/completions")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("Authorization", "Bearer $apiKey")
    connection.doOutput = true

    val body = JSONObject().apply {
        put("model", "gpt-4o-mini")
        put("messages", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are an expert nutritionist. Always respond in valid JSON. Values must be plain text, never nested JSON.")
            })
            put(JSONObject().apply { put("role", "user"); put("content", prompt) })
        })
        put("max_tokens", 1500)
        put("temperature", 0.7)
    }.toString()

    connection.outputStream.write(body.toByteArray())
    val response = connection.inputStream.bufferedReader().readText()
    JSONObject(response).getJSONArray("choices")
        .getJSONObject(0).getJSONObject("message").getString("content")
}