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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitaai.app.R
import com.vitaai.app.icons.IconList
import com.vitaai.app.ui.theme.Gold
import com.vitaai.app.ui.theme.NavyBlue
import com.vitaai.app.utils.XPManager
import com.vitaai.app.utils.callOpenAI
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

private const val USERS_COLLECTION = "users"
private const val FIELD_DIET = "dietPlan"
private const val FIELD_EXERCISE = "exercisePlan"

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
    val langName = Locale.getDefault().displayLanguage

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
                        .collection(USERS_COLLECTION).document(uid)
                        .update(mapOf(
                            FIELD_DIET to dietPlan,
                            FIELD_EXERCISE to exercisePlan,
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
        Text(
            text = stringResource(R.string.plan_current_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.plan_summary_format, age, weight, height, imcFormatted),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(24.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.plan_generating),
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
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
                        Icon(imageVector = IconList.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.common_saved),
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.common_xp_reward, XPManager.XP_GENERATE_PLAN),
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(imageVector = IconList.Achievements, contentDescription = null, modifier = Modifier.size(14.dp), tint = Gold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = IconList.Diet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.plan_diet_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(dietPlan, fontSize = 14.sp, lineHeight = 22.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = IconList.Exercise, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.plan_exercise_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                Text(
                    text = stringResource(R.string.plan_back_home),
                    fontSize = 16.sp, color = Color.White
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
