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
import com.vitaai.app.utils.callOpenAI
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun PlanAdjustScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var isAnalyzing by remember { mutableStateOf(false) }
    var analysis by remember { mutableStateOf("") }
    var newDietPlan by remember { mutableStateOf("") }
    var newExercisePlan by remember { mutableStateOf("") }
    var savedMsg by remember { mutableStateOf("") }
    var userGoal by remember { mutableStateOf("") }
    var progressData by remember { mutableStateOf<List<ProgressEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                userGoal = doc.getString("goal") ?: ""
            }

        db.collection("users").document(uid)
            .collection("progress")
            .orderBy("date")
            .limit(14)
            .get()
            .addOnSuccessListener { snap ->
                progressData = snap.documents.mapNotNull {
                    it.toObject(ProgressEntry::class.java)
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = IconList.Progress, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.plan_adjust_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.plan_adjust_subtitle),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        if (progressData.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = IconList.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.plan_adjust_recent),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    val firstWeight = progressData.first().weight
                    val lastWeight = progressData.last().weight
                    val weightChange = lastWeight - firstWeight
                    val avgCalories = progressData.map { it.calories }.average()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard(stringResource(R.string.plan_adjust_stat_start), "${firstWeight}kg")
                        StatCard(stringResource(R.string.plan_adjust_stat_current), "${lastWeight}kg")
                        StatCard(
                            stringResource(R.string.plan_adjust_stat_change),
                            "${if (weightChange >= 0) "+" else ""}${"%.1f".format(weightChange)}kg"
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.plan_adjust_avg_calories, avgCalories.toInt()),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.plan_adjust_goal_label, userGoal),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    isAnalyzing = true
                    analysis = ""
                    newDietPlan = ""
                    newExercisePlan = ""
                    scope.launch {
                        try {
                            val firstWeight = progressData.first().weight
                            val lastWeight = progressData.last().weight
                            val weightChange = lastWeight - firstWeight
                            val avgCalories = progressData.map { it.calories }.average()
                            val days = progressData.size

                            val prompt = """
                                Eres un nutricionista y entrenador personal experto.
                                
                                DATOS DEL USUARIO:
                                - Objetivo: $userGoal
                                - Peso inicial (hace $days días): ${firstWeight}kg
                                - Peso actual: ${lastWeight}kg
                                - Cambio de peso: ${weightChange}kg
                                - Calorías promedio diarias: ${avgCalories.toInt()} kcal
                                
                                ANÁLISIS REQUERIDO:
                                Basándote en estos datos reales, responde SOLO en este JSON exacto:
                                {
                                  "analysis": "análisis breve del progreso en 2 frases",
                                  "diet": "nuevo plan de dieta ajustado y específico",
                                  "exercise": "nuevo plan de ejercicio ajustado y específico"
                                }
                            """.trimIndent()

                            val result = callOpenAI(prompt)
                            val json = JSONObject(result)
                            analysis = json.getString("analysis")
                            newDietPlan = json.getString("diet")
                            newExercisePlan = json.getString("exercise")
                        } catch (e: Exception) {
                            analysis = "Error: ${e.message}"
                        }
                        isAnalyzing = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isAnalyzing
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.plan_adjust_analyzing))
                } else {
                    Icon(imageVector = IconList.Nutritionist, contentDescription = null, tint = IconList.RobotTeal, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.plan_adjust_analyze_button), fontSize = 16.sp)
                }
            }

            if (analysis.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = IconList.Tip, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.plan_adjust_analysis_label), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(analysis, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = IconList.Diet, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.plan_adjust_diet_label), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(newDietPlan, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = IconList.Exercise, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.plan_adjust_exercise_label), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(newExercisePlan, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (savedMsg.isNotEmpty()) {
                    Text(savedMsg, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        db.collection("users").document(uid)
                            .update(mapOf(
                                "dietPlan" to newDietPlan,
                                "exercisePlan" to newExercisePlan
                            ))
                            .addOnSuccessListener {
                                savedMsg = "Plan actualizado"
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = IconList.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.plan_adjust_save_button), fontSize = 16.sp)
                }
            }

        } else {
            Spacer(Modifier.height(48.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = IconList.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.plan_adjust_no_data),
                        fontSize = 16.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.plan_adjust_no_data_desc),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
    }
}
